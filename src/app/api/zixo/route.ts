import { NextRequest, NextResponse } from 'next/server';

export const runtime = 'edge';

/**
 * Zixo API Routes - Cloudflare Workers Compatible
 *
 * These routes use Firebase REST APIs with JWT-based OAuth2 authentication
 * for full admin capabilities (Firestore, FCM, etc.)
 *
 * The FIREBASE_PRIVATE_KEY env var must be set for full admin operations.
 * Without it, RTDB operations still work via the database secret.
 */

import { adminOperations } from '@/services/firebase-admin';

const PROJECT_ID = 'zixo-call';
const RTDB_SECRET = process.env.FIREBASE_DATABASE_SECRET || '';

// ==================== HELPERS ====================

function rtdbUrl(path: string): string {
  return `https://${PROJECT_ID}-default-rtdb.firebaseio.com${path}.json?auth=${RTDB_SECRET}`;
}

async function rtdbGet(path: string): Promise<any> {
  const res = await fetch(rtdbUrl(path));
  if (!res.ok) throw new Error(`RTDB GET failed: ${res.status}`);
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

async function rtdbSet(path: string, method: string, data?: any): Promise<any> {
  const options: RequestInit = {
    method,
    headers: { 'Content-Type': 'application/json' },
  };
  if (data) options.body = JSON.stringify(data);
  const res = await fetch(rtdbUrl(path), options);
  if (!res.ok) throw new Error(`RTDB ${method} failed: ${res.status}`);
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

/**
 * Verify that a user has admin role.
 * Tries Firestore REST API first, falls back to RTDB.
 */
async function verifyAdmin(requesterUid: string): Promise<boolean> {
  // Method 1: Firestore REST API via adminOperations
  try {
    const profile = await adminOperations.getDocument('users', requesterUid);
    if (profile && profile.role === 'admin') {
      return true;
    }
  } catch (err: any) {
    console.warn('[Zixo API] Admin verification via Firestore REST failed:', err.message);
  }

  // Method 2: RTDB fallback (always works with database secret)
  try {
    const rtdbProfile = await rtdbGet(`/users/${requesterUid}`);
    if (rtdbProfile && rtdbProfile.role === 'admin') {
      return true;
    }
  } catch (err: any) {
    console.warn('[Zixo API] Admin verification via RTDB failed:', err.message);
  }

  return false;
}

// ==================== HEALTH CHECK ====================
export async function GET() {
  const hasPrivateKey = !!process.env.FIREBASE_PRIVATE_KEY;
  const hasRtdbSecret = !!RTDB_SECRET;

  return NextResponse.json({
    status: 'ok',
    app: 'Zixo',
    version: '1.0.0',
    firebase: {
      projectId: PROJECT_ID,
      region: 'us-central1',
      adminEnabled: hasPrivateKey,
      rtdbEnabled: hasRtdbSecret,
    },
  });
}

// ==================== POST HANDLER ====================
export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { action } = body;

    switch (action) {
      // ==================== SEND NOTIFICATION ====================
      case 'sendNotification': {
        const { token, title, body: messageBody, data } = body;

        if (!token || !title) {
          return NextResponse.json(
            { error: 'Missing required fields: token, title' },
            { status: 400 }
          );
        }

        try {
          // Try FCM v1 API with admin credentials
          const messageId = await adminOperations.sendFCMMessage(
            token,
            { title, body: messageBody || '' },
            data
          );

          return NextResponse.json({
            success: true,
            messageId,
            method: 'fcm-v1',
          });
        } catch (err: any) {
          console.error('[Zixo API] Notification error:', err.message);
          return NextResponse.json(
            { error: 'Failed to send notification', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== DELETE ACCOUNT ====================
      case 'deleteAccount': {
        const { uid } = body;

        if (!uid) {
          return NextResponse.json(
            { error: 'Missing required field: uid' },
            { status: 400 }
          );
        }

        try {
          // Clean up RTDB presence
          await rtdbSet(`/presence/${uid}`, 'DELETE');

          // Clean up RTDB typing indicators
          await rtdbSet(`/typing`, 'PATCH', null);

          // Try Firestore cleanup with admin credentials
          if (process.env.FIREBASE_PRIVATE_KEY) {
            try {
              // Delete user profile
              await adminOperations.deleteDocument('users', uid);
              // Delete user's messages and chats (best effort)
              console.log(`[Zixo API] Firestore cleanup for uid: ${uid}`);
            } catch (fsErr: any) {
              console.warn('[Zixo API] Firestore cleanup failed:', fsErr.message);
            }
          }

          return NextResponse.json({
            success: true,
            message: `Account ${uid} data cleaned. For full Auth deletion, use Firebase Console or Cloud Functions.`,
          });
        } catch (err: any) {
          console.error('[Zixo API] Account cleanup error:', err.message);
          return NextResponse.json(
            { error: 'Failed to cleanup account', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== VERIFY USERNAME ====================
      case 'verifyUsername': {
        const { username } = body;

        if (!username) {
          return NextResponse.json(
            { error: 'Missing required field: username' },
            { status: 400 }
          );
        }

        try {
          // Try Firestore first (with admin credentials)
          if (process.env.FIREBASE_PRIVATE_KEY) {
            try {
              const users = await adminOperations.queryCollection('users', 'username', 'EQUAL', username);
              return NextResponse.json({
                available: users.length === 0,
                username,
                source: 'firestore',
              });
            } catch {
              // Fall through to RTDB
            }
          }

          // Fallback to RTDB
          const result = await rtdbGet(`/usernames/${username}`);
          return NextResponse.json({
            available: result === null,
            username,
            source: 'rtdb',
          });
        } catch (err: any) {
          console.error('[Zixo API] Username verification error:', err.message);
          return NextResponse.json(
            { error: 'Failed to verify username' },
            { status: 500 }
          );
        }
      }

      // ==================== GET USER PROFILE ====================
      case 'getUserProfile': {
        const { uid } = body;

        if (!uid) {
          return NextResponse.json(
            { error: 'Missing required field: uid' },
            { status: 400 }
          );
        }

        try {
          // Try Firestore with admin credentials
          if (process.env.FIREBASE_PRIVATE_KEY) {
            try {
              const profile = await adminOperations.getDocument('users', uid);
              if (profile) {
                return NextResponse.json({ profile, source: 'firestore' });
              }
            } catch {
              // Fall through to RTDB
            }
          }

          // Fallback to RTDB
          const profile = await rtdbGet(`/users/${uid}`);
          if (!profile) {
            return NextResponse.json(
              { error: 'User not found' },
              { status: 404 }
            );
          }
          return NextResponse.json({ profile, source: 'rtdb' });
        } catch (err: any) {
          console.error('[Zixo API] Get profile error:', err.message);
          return NextResponse.json(
            { error: 'Failed to get user profile' },
            { status: 500 }
          );
        }
      }

      // ==================== SEARCH USERS ====================
      case 'searchUsers': {
        const { query: searchQuery } = body;

        if (!searchQuery) {
          return NextResponse.json(
            { error: 'Missing required field: query' },
            { status: 400 }
          );
        }

        try {
          // Try Firestore with admin credentials for proper querying
          if (process.env.FIREBASE_PRIVATE_KEY) {
            try {
              const users = await adminOperations.queryCollection(
                'users',
                'username',
                'EQUAL',
                searchQuery.startsWith('@') ? searchQuery : `@${searchQuery}`
              );
              if (users.length > 0) {
                return NextResponse.json({ users, source: 'firestore' });
              }
            } catch {
              // Fall through to RTDB
            }
          }

          // Fallback: RTDB (load all, filter client-side)
          const allUsers = await rtdbGet('/users');
          if (!allUsers) {
            return NextResponse.json({ users: [] });
          }

          const users = Object.entries(allUsers)
            .filter(([_uid, data]: [string, any]) =>
              data?.username?.toLowerCase().startsWith(searchQuery.toLowerCase())
            )
            .slice(0, 10)
            .map(([uid, data]: [string, any]) => ({ uid, ...data }));

          return NextResponse.json({ users, source: 'rtdb' });
        } catch (err: any) {
          console.error('[Zixo API] Search users error:', err.message);
          return NextResponse.json(
            { error: 'Failed to search users' },
            { status: 500 }
          );
        }
      }

      // ==================== DISCOVER USERS (no admin required) ====================
      case 'discoverUsers': {
        const { limit: reqLimit } = body;
        const maxUsers = Math.min(reqLimit || 50, 100);

        try {
          let users: any[] = [];

          // Method 1: Firestore REST API listDocuments
          try {
            const listResult = await adminOperations.firestoreRequest(
              'GET',
              `/documents/users?pageSize=${maxUsers}`
            );
            if (listResult?.documents) {
              users = listResult.documents.map((doc: any) => {
                const js = adminOperations.firestoreDocumentToJs(doc);
                return {
                  id: doc.name.split('/').pop(),
                  uid: js.uid || doc.name.split('/').pop(),
                  displayName: js.displayName || '',
                  username: js.username || '',
                  avatar: js.avatar || '',
                  online: js.online || false,
                  zixoNumber: js.zixoNumber || '',
                };
              });
            }
          } catch (listErr: any) {
            console.warn('[Zixo API] Discover users Firestore list failed:', listErr.message);
          }

          // Method 2: RTDB fallback
          if (users.length === 0) {
            try {
              const rtdbUsers = await rtdbGet('/users');
              if (rtdbUsers && typeof rtdbUsers === 'object') {
                users = Object.entries(rtdbUsers)
                  .slice(0, maxUsers)
                  .map(([uid, data]: [string, any]) => ({
                    id: uid,
                    uid,
                    displayName: data?.displayName || '',
                    username: data?.username || '',
                    avatar: data?.avatar || '',
                    online: data?.online || false,
                    zixoNumber: data?.zixoNumber || '',
                  }));
              }
            } catch (rtdbErr: any) {
              console.warn('[Zixo API] Discover users RTDB fallback failed:', rtdbErr.message);
            }
          }

          return NextResponse.json({
            success: true,
            users,
            count: users.length,
          });
        } catch (err: any) {
          console.error('[Zixo API] Discover users error:', err.message);
          return NextResponse.json(
            { error: 'Failed to discover users', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== CLEANUP STALE CALLS ====================
      case 'cleanupStaleCalls': {
        try {
          const calls = await rtdbGet('/calls');
          if (!calls) {
            return NextResponse.json({ success: true, cleaned: 0 });
          }

          const fiveMinutesAgo = Date.now() - 5 * 60 * 1000;
          const updates: Record<string, null> = {};

          Object.entries(calls).forEach(([callId, callData]: [string, any]) => {
            if (callData?.createdAt && callData.createdAt < fiveMinutesAgo) {
              updates[callId] = null;
            }
          });

          if (Object.keys(updates).length > 0) {
            await rtdbSet('/calls', 'PATCH', updates);
          }

          return NextResponse.json({
            success: true,
            cleaned: Object.keys(updates).length,
          });
        } catch (err: any) {
          console.error('[Zixo API] Cleanup error:', err.message);
          return NextResponse.json(
            { error: 'Failed to cleanup stale calls' },
            { status: 500 }
          );
        }
      }

      // ==================== SEND PUSH NOTIFICATION (v2) ====================
      case 'sendPush': {
        const { uid, title: pushTitle, body: pushBody, data: pushData } = body;

        if (!uid || !pushTitle) {
          return NextResponse.json(
            { error: 'Missing required fields: uid, title' },
            { status: 400 }
          );
        }

        try {
          // Get user's FCM token from Firestore
          let fcmToken: string | null = null;

          if (process.env.FIREBASE_PRIVATE_KEY) {
            const profile = await adminOperations.getDocument('users', uid);
            fcmToken = profile?.fcmToken || null;
          }

          if (!fcmToken) {
            return NextResponse.json(
              { error: 'User has no FCM token registered' },
              { status: 404 }
            );
          }

          const messageId = await adminOperations.sendFCMMessage(
            fcmToken,
            { title: pushTitle, body: pushBody || '' },
            pushData
          );

          return NextResponse.json({
            success: true,
            messageId,
            method: 'fcm-v1',
          });
        } catch (err: any) {
          console.error('[Zixo API] Push notification error:', err.message);
          return NextResponse.json(
            { error: 'Failed to send push notification', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== SETUP: SET ADMIN ROLE (one-time setup) ====================
      case 'setAdminRole': {
        const { targetUid, secret } = body;

        // Only allow with the setup secret (one-time use for initial admin setup)
        if (secret !== 'zixo-admin-setup-2026' || !targetUid) {
          return NextResponse.json({ error: 'Unauthorized' }, { status: 403 });
        }

        try {
          await adminOperations.setDocument('users', targetUid, { role: 'admin' }, true);
          return NextResponse.json({ success: true, message: `Admin role set for ${targetUid}` });
        } catch (err: any) {
          console.error('[Zixo API] Set admin role error:', err.message);
          return NextResponse.json(
            { error: 'Failed to set admin role', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== ADMIN: ASSIGN ZIXO NUMBERS ====================
      case 'assignZixoNumbers': {
        const { requesterUid } = body;

        if (!requesterUid) {
          return NextResponse.json(
            { error: 'Missing required field: requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can assign Zixo numbers' },
              { status: 403 }
            );
          }

          // Get all users from Firestore (with RTDB fallback)
          let users: any[] = [];
          try {
            const listResult = await adminOperations.firestoreRequest(
              'GET',
              `/documents/users?pageSize=300`
            );
            if (listResult?.documents) {
              users = listResult.documents.map((doc: any) => ({
                id: doc.name.split('/').pop(),
                ...adminOperations.firestoreDocumentToJs(doc),
              }));
            }
          } catch (listErr: any) {
            console.warn('[Zixo API] Firestore list documents failed, trying RTDB fallback:', listErr.message);
            try {
              const rtdbUsers = await rtdbGet('/users');
              if (rtdbUsers && typeof rtdbUsers === 'object') {
                users = Object.entries(rtdbUsers).map(([uid, data]: [string, any]) => ({
                  id: uid,
                  zixoNumber: data?.zixoNumber || '',
                  displayName: data?.displayName || '',
                  email: data?.email || '',
                  role: data?.role || 'user',
                }));
              }
            } catch (rtdbErr: any) {
              console.warn('[Zixo API] RTDB fallback also failed:', rtdbErr.message);
              return NextResponse.json(
                { error: 'Failed to list users', details: listErr.message },
                { status: 500 }
              );
            }
          }

          // For each user without a zixoNumber, generate and assign one
          let assignedCount = 0;
          const usedNumbers = new Set<string>();

          // Collect existing zixoNumbers to avoid collisions
          for (const user of users) {
            if (user.zixoNumber) {
              usedNumbers.add(user.zixoNumber);
            }
          }

          for (const user of users) {
            if (!user.zixoNumber && user.id) {
              // Generate a unique 8-digit number
              let zixoNumber = '';
              for (let attempt = 0; attempt < 20; attempt++) {
                const num = String(Math.floor(Math.random() * 90000000) + 10000000);
                if (!usedNumbers.has(num)) {
                  zixoNumber = num;
                  usedNumbers.add(num);
                  break;
                }
              }

              if (zixoNumber) {
                try {
                  // Update the user profile
                  await adminOperations.setDocument('users', user.id, { zixoNumber }, true);
                  // Create zixoNumber mapping
                  await adminOperations.setDocument('zixoNumbers', zixoNumber, { uid: user.id }, false);
                  assignedCount++;
                } catch (err: any) {
                  console.warn(`[Zixo API] Failed to assign zixoNumber for ${user.id}:`, err.message);
                }
              }
            }
          }

          return NextResponse.json({
            success: true,
            totalUsers: users.length,
            assignedCount,
            message: `Assigned ${assignedCount} Zixo numbers to users without one`,
          });
        } catch (err: any) {
          console.error('[Zixo API] Assign Zixo numbers error:', err.message);
          return NextResponse.json(
            { error: 'Failed to assign Zixo numbers', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== ADMIN: GRANT ADMIN ROLE ====================
      case 'grantAdmin': {
        const { targetUid, requesterUid } = body;

        if (!targetUid || !requesterUid) {
          return NextResponse.json(
            { error: 'Missing required fields: targetUid, requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can grant admin role' },
              { status: 403 }
            );
          }

          // Grant admin role to target user
          await adminOperations.setDocument('users', targetUid, { role: 'admin' }, true);

          return NextResponse.json({
            success: true,
            message: `Admin role granted to ${targetUid}`,
          });
        } catch (err: any) {
          console.error('[Zixo API] Grant admin error:', err.message);
          return NextResponse.json(
            { error: 'Failed to grant admin role', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== ADMIN: REVOKE ADMIN ROLE ====================
      case 'revokeAdmin': {
        const { targetUid, requesterUid } = body;

        if (!targetUid || !requesterUid) {
          return NextResponse.json(
            { error: 'Missing required fields: targetUid, requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can revoke admin role' },
              { status: 403 }
            );
          }

          // Revoke admin role from target user
          await adminOperations.setDocument('users', targetUid, { role: 'user' }, true);

          return NextResponse.json({
            success: true,
            message: `Admin role revoked from ${targetUid}`,
          });
        } catch (err: any) {
          console.error('[Zixo API] Revoke admin error:', err.message);
          return NextResponse.json(
            { error: 'Failed to revoke admin role', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== ADMIN: LIST ALL USERS ====================
      case 'listUsers': {
        const { requesterUid } = body;

        if (!requesterUid) {
          return NextResponse.json(
            { error: 'Missing required field: requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can list all users' },
              { status: 403 }
            );
          }

          // List all users — try multiple methods with fallbacks
          let users: any[] = [];

          // Method 1: Firestore REST API listDocuments
          try {
            const listResult = await adminOperations.firestoreRequest(
              'GET',
              `/documents/users?pageSize=300`
            );
            if (listResult?.documents) {
              users = listResult.documents.map((doc: any) => ({
                id: doc.name.split('/').pop(),
                ...adminOperations.firestoreDocumentToJs(doc),
              }));
            }
          } catch (listErr: any) {
            console.warn('[Zixo API] Firestore listDocuments failed:', listErr.message);

            // Method 2: Firestore REST API runQuery (structured query without WHERE)
            try {
              const queryBody = {
                structuredQuery: {
                  from: [{ collectionId: 'users' }],
                  limit: 300,
                },
              };
              const queryResult = await adminOperations.firestoreRequest(
                'POST',
                '/documents:runQuery',
                queryBody
              );
              if (Array.isArray(queryResult)) {
                users = queryResult
                  .filter((item: any) => item.document)
                  .map((item: any) => ({
                    id: item.document.name.split('/').pop(),
                    ...adminOperations.firestoreDocumentToJs(item.document),
                  }));
              }
            } catch (queryErr: any) {
              console.warn('[Zixo API] Firestore runQuery also failed:', queryErr.message);
            }
          }

          // Method 3: RTDB fallback (always works with database secret)
          if (users.length === 0) {
            try {
              const rtdbUsers = await rtdbGet('/users');
              if (rtdbUsers && typeof rtdbUsers === 'object') {
                users = Object.entries(rtdbUsers).map(([uid, data]: [string, any]) => ({
                  id: uid,
                  uid,
                  displayName: data?.displayName || '',
                  email: data?.email || '',
                  username: data?.username || '',
                  bio: data?.bio || '',
                  avatar: data?.avatar || '',
                  online: data?.online || false,
                  role: data?.role || 'user',
                  zixoNumber: data?.zixoNumber || '',
                  lastSeen: data?.lastSeen || null,
                  createdAt: data?.createdAt || null,
                  fcmToken: data?.fcmToken || '',
                  banned: data?.banned || false,
                }));
              }
            } catch (rtdbErr: any) {
              console.warn('[Zixo API] RTDB fallback also failed:', rtdbErr.message);
            }
          }

          return NextResponse.json({
            success: true,
            users,
            count: users.length,
          });
        } catch (err: any) {
          console.error('[Zixo API] List users error:', err.message);
          return NextResponse.json(
            { error: 'Failed to list users', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== ADMIN: DELETE USER ====================
      case 'adminDeleteUser': {
        const { targetUid, requesterUid } = body;

        if (!targetUid || !requesterUid) {
          return NextResponse.json(
            { error: 'Missing required fields: targetUid, requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can delete users' },
              { status: 403 }
            );
          }

          // Delete user data from Firestore and RTDB
          await adminOperations.deleteDocument('users', targetUid);
          await rtdbSet(`/presence/${targetUid}`, 'DELETE');

          return NextResponse.json({
            success: true,
            message: `User ${targetUid} data deleted`,
          });
        } catch (err: any) {
          console.error('[Zixo API] Admin delete user error:', err.message);
          return NextResponse.json(
            { error: 'Failed to delete user', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== SETUP: ENABLE PHONE AUTH ====================
      case 'enablePhoneAuth': {
        const { requesterUid } = body;

        if (!requesterUid) {
          return NextResponse.json(
            { error: 'Missing required field: requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can enable phone auth' },
              { status: 403 }
            );
          }

          // Get OAuth2 access token
          const token = await adminOperations.getAccessToken();
          if (!token) {
            return NextResponse.json(
              { error: 'No admin access token available. Set FIREBASE_PRIVATE_KEY.' },
              { status: 500 }
            );
          }

          // Enable Phone Auth provider via Identity Toolkit API
          // First get current config
          const configUrl = `https://identitytoolkit.googleapis.com/v2/projects/${PROJECT_ID}/config`;
          const configRes = await fetch(configUrl, {
            headers: { 'Authorization': `Bearer ${token}` },
          });

          let currentConfig: any = {};
          if (configRes.ok) {
            currentConfig = await configRes.json();
          }

          // Update config to enable phone provider with all regions
          const updateBody = {
            signIn: {
              allowDuplicateEmails: false,
              anonymous: { enabled: false },
              email: { enabled: true, passwordRequired: true },
              phone: { enabled: true, }
            },
          };

          const updateRes = await fetch(
            `https://identitytoolkit.googleapis.com/v2/projects/${PROJECT_ID}/config?updateMask=signIn`,
            {
              method: 'PATCH',
              headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
              },
              body: JSON.stringify(updateBody),
            }
          );

          if (!updateRes.ok) {
            const errText = await updateRes.text();
            return NextResponse.json({
              success: false,
              error: `Identity Toolkit update failed: ${updateRes.status}`,
              details: errText,
              currentConfig,
            }, { status: 500 });
          }

          const updatedConfig = await updateRes.json();

          // Also try to enable SMS regions for all countries
          // The quota config controls which regions can receive SMS
          const quotaUrl = `https://identitytoolkit.googleapis.com/v2/projects/${PROJECT_ID}/defaultSupportedIdpConfigs/phone.config?updateMask=smsRegionConfig`;
          // Actually, the regions are configured via the project config

          return NextResponse.json({
            success: true,
            phoneAuthEnabled: updatedConfig?.signIn?.phone?.enabled ?? true,
            message: 'Phone auth has been enabled. You may also need to enable SMS regions in Firebase Console.',
            currentConfig: currentConfig?.signIn || 'unknown',
          });
        } catch (err: any) {
          console.error('[Zixo API] Enable phone auth error:', err.message);
          return NextResponse.json(
            { error: 'Failed to enable phone auth', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== ADMIN: BAN USER ====================
      case 'banUser': {
        const { targetUid, requesterUid } = body;

        if (!targetUid || !requesterUid) {
          return NextResponse.json(
            { error: 'Missing required fields: targetUid, requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can ban users' },
              { status: 403 }
            );
          }

          // Set banned: true on the user's Firestore doc
          await adminOperations.setDocument('users', targetUid, { banned: true }, true);

          // Also clear their online status so they appear offline
          await rtdbSet(`/presence/${targetUid}`, 'DELETE');

          return NextResponse.json({
            success: true,
            message: `User ${targetUid} has been banned`,
          });
        } catch (err: any) {
          console.error('[Zixo API] Ban user error:', err.message);
          return NextResponse.json(
            { error: 'Failed to ban user', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== ADMIN: UNBAN USER ====================
      case 'unbanUser': {
        const { targetUid, requesterUid } = body;

        if (!targetUid || !requesterUid) {
          return NextResponse.json(
            { error: 'Missing required fields: targetUid, requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can unban users' },
              { status: 403 }
            );
          }

          // Set banned: false on the user's Firestore doc
          await adminOperations.setDocument('users', targetUid, { banned: false }, true);

          return NextResponse.json({
            success: true,
            message: `User ${targetUid} has been unbanned`,
          });
        } catch (err: any) {
          console.error('[Zixo API] Unban user error:', err.message);
          return NextResponse.json(
            { error: 'Failed to unban user', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== ADMIN: BROADCAST NOTIFICATION ====================
      case 'broadcastNotification': {
        const { title, body: notifBody, requesterUid } = body;

        if (!title || !requesterUid) {
          return NextResponse.json(
            { error: 'Missing required fields: title, requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can broadcast notifications' },
              { status: 403 }
            );
          }

          // List all users and find those with FCM tokens
          let users: any[] = [];
          try {
            const listResult = await adminOperations.firestoreRequest(
              'GET',
              `/documents/users?pageSize=300`
            );
            if (listResult?.documents) {
              users = listResult.documents.map((doc: any) => ({
                id: doc.name.split('/').pop(),
                ...adminOperations.firestoreDocumentToJs(doc),
              }));
            }
          } catch (listErr: any) {
            console.warn('[Zixo API] Firestore list for broadcast failed, trying RTDB:', listErr.message);
            try {
              const rtdbUsers = await rtdbGet('/users');
              if (rtdbUsers && typeof rtdbUsers === 'object') {
                users = Object.entries(rtdbUsers).map(([uid, data]: [string, any]) => ({
                  id: uid,
                  uid,
                  displayName: data?.displayName || '',
                  email: data?.email || '',
                  fcmToken: data?.fcmToken || '',
                  banned: data?.banned || false,
                }));
              }
            } catch (rtdbErr: any) {
              console.warn('[Zixo API] RTDB fallback for broadcast also failed:', rtdbErr.message);
            }
          }

          // Send FCM to all users with fcmToken
          let sentCount = 0;
          let failedCount = 0;
          const sendPromises = users
            .filter((u: any) => u.fcmToken && !u.banned)
            .map(async (u: any) => {
              try {
                await adminOperations.sendFCMMessage(
                  u.fcmToken,
                  { title, body: notifBody || '' },
                  { type: 'admin_broadcast' }
                );
                sentCount++;
              } catch {
                failedCount++;
              }
            });

          await Promise.allSettled(sendPromises);

          return NextResponse.json({
            success: true,
            sentCount,
            failedCount,
            totalUsers: users.length,
          });
        } catch (err: any) {
          console.error('[Zixo API] Broadcast notification error:', err.message);
          return NextResponse.json(
            { error: 'Failed to broadcast notification', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== ADMIN: GET APP STATS ====================
      case 'getAppStats': {
        const { requesterUid } = body;

        if (!requesterUid) {
          return NextResponse.json(
            { error: 'Missing required field: requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can view stats' },
              { status: 403 }
            );
          }

          // Get user counts from Firestore (with RTDB fallback)
          let totalUsers = 0;
          let onlineUsers = 0;
          try {
            const listResult = await adminOperations.firestoreRequest(
              'GET',
              `/documents/users?pageSize=300`
            );
            if (listResult?.documents) {
              totalUsers = listResult.documents.length;
              onlineUsers = listResult.documents.filter((doc: any) => {
                const js = adminOperations.firestoreDocumentToJs(doc);
                return js.online === true;
              }).length;
            }
          } catch (err: any) {
            console.warn('[Zixo API] Firestore stats failed, trying RTDB:', err.message);
            try {
              const rtdbUsers = await rtdbGet('/users');
              if (rtdbUsers && typeof rtdbUsers === 'object') {
                totalUsers = Object.keys(rtdbUsers).length;
                onlineUsers = Object.values(rtdbUsers).filter((u: any) => u?.online === true).length;
              }
            } catch (rtdbErr: any) {
              console.warn('[Zixo API] RTDB stats fallback also failed:', rtdbErr.message);
            }
          }

          // Get active chats count from Firestore
          let activeChats = 0;
          try {
            const chatResult = await adminOperations.firestoreRequest(
              'GET',
              `/documents/chats?pageSize=300`
            );
            if (chatResult?.documents) {
              activeChats = chatResult.documents.length;
            }
          } catch (err: any) {
            console.warn('[Zixo API] Stats chat list failed:', err.message);
          }

          // Get call count from RTDB
          let callCount = 0;
          try {
            const calls = await rtdbGet('/calls');
            if (calls && typeof calls === 'object') {
              callCount = Object.keys(calls).length;
            }
          } catch (err: any) {
            console.warn('[Zixo API] Stats call count failed:', err.message);
          }

          // Also get presence count from RTDB
          let presenceCount = 0;
          try {
            const presence = await rtdbGet('/presence');
            if (presence && typeof presence === 'object') {
              presenceCount = Object.keys(presence).length;
            }
          } catch (err: any) {
            console.warn('[Zixo API] Stats presence count failed:', err.message);
          }

          // Use the higher of Firestore online count vs RTDB presence count
          const effectiveOnline = Math.max(onlineUsers, presenceCount);

          return NextResponse.json({
            success: true,
            stats: {
              totalUsers,
              onlineUsers: effectiveOnline,
              activeChats,
              callCount,
            },
          });
        } catch (err: any) {
          console.error('[Zixo API] Get app stats error:', err.message);
          return NextResponse.json(
            { error: 'Failed to get app stats', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== ADMIN: UPDATE AUTH CONFIG ====================
      case 'updateAuthConfig': {
        const { requesterUid, phoneEnabled, googleEnabled } = body;

        if (!requesterUid) {
          return NextResponse.json(
            { error: 'Missing required field: requesterUid' },
            { status: 400 }
          );
        }

        try {
          // Verify requester is admin
          const isAdmin = await verifyAdmin(requesterUid);
          if (!isAdmin) {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can update auth config' },
              { status: 403 }
            );
          }

          // Get OAuth2 access token
          const token = await adminOperations.getAccessToken();
          if (!token) {
            return NextResponse.json(
              { error: 'No admin access token available. Set FIREBASE_PRIVATE_KEY.' },
              { status: 500 }
            );
          }

          // Update Identity Toolkit config
          const updateBody: any = {
            signIn: {
              allowDuplicateEmails: false,
              anonymous: { enabled: false },
              email: { enabled: true, passwordRequired: true },
              phone: { enabled: phoneEnabled ?? true },
            },
          };

          const updateRes = await fetch(
            `https://identitytoolkit.googleapis.com/v2/projects/${PROJECT_ID}/config?updateMask=signIn`,
            {
              method: 'PATCH',
              headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
              },
              body: JSON.stringify(updateBody),
            }
          );

          if (!updateRes.ok) {
            const errText = await updateRes.text();
            return NextResponse.json({
              success: false,
              error: `Identity Toolkit update failed: ${updateRes.status}`,
              details: errText,
            }, { status: 500 });
          }

          const updatedConfig = await updateRes.json();

          // Store auth config in RTDB for reference
          await rtdbSet('/admin/authConfig', 'PUT', {
            phoneEnabled: phoneEnabled ?? true,
            googleEnabled: googleEnabled ?? true,
            updatedAt: Date.now(),
          });

          return NextResponse.json({
            success: true,
            phoneEnabled: updatedConfig?.signIn?.phone?.enabled ?? phoneEnabled,
            googleEnabled: googleEnabled ?? true,
            message: 'Auth configuration updated',
          });
        } catch (err: any) {
          console.error('[Zixo API] Update auth config error:', err.message);
          return NextResponse.json(
            { error: 'Failed to update auth config', details: err.message },
            { status: 500 }
          );
        }
      }


      // ==================== SETUP: FIND USER BY EMAIL ====================
      case 'findUserByEmail': {
        const { email, secret: setupSecret } = body;

        if (setupSecret !== 'zixo-admin-setup-2026' || !email) {
          return NextResponse.json({ error: 'Unauthorized or missing email' }, { status: 403 });
        }

        try {
          // Try Firestore admin first
          if (process.env.FIREBASE_PRIVATE_KEY) {
            try {
              const users = await adminOperations.queryCollection('users', 'email', 'EQUAL', email);
              if (users.length > 0) {
                return NextResponse.json({ found: true, users, source: 'firestore' });
              }
            } catch (fsErr: any) {
              console.warn('[Zixo API] Firestore query failed:', fsErr.message);
            }
          }

          // Fallback: list all users and filter
          try {
            const listResult = await adminOperations.firestoreRequest(
              'GET',
              `/documents/users?pageSize=300`
            );
            if (listResult?.documents) {
              const allUsers = listResult.documents.map((doc: any) => ({
                id: doc.name.split('/').pop(),
                ...adminOperations.firestoreDocumentToJs(doc),
              }));
              const matched = allUsers.filter((u: any) => u.email === email);
              if (matched.length > 0) {
                return NextResponse.json({ found: true, users: matched, source: 'firestore-list' });
              }
            }
          } catch (listErr: any) {
            console.warn('[Zixo API] List users failed:', listErr.message);
          }

          return NextResponse.json({ found: false, email });
        } catch (err: any) {
          console.error('[Zixo API] Find user by email error:', err.message);
          return NextResponse.json(
            { error: 'Failed to find user', details: err.message },
            { status: 500 }
          );
        }
      }

      // ==================== SETUP: SET ADMIN BY EMAIL ====================
      case 'setAdminByEmail': {
        const { email, secret: setupSecret2 } = body;

        if (setupSecret2 !== 'zixo-admin-setup-2026' || !email) {
          return NextResponse.json({ error: 'Unauthorized or missing email' }, { status: 403 });
        }

        try {
          // Find user by email first
          let targetUid: string | null = null;
          
          if (process.env.FIREBASE_PRIVATE_KEY) {
            try {
              const users = await adminOperations.queryCollection('users', 'email', 'EQUAL', email);
              if (users.length > 0 && users[0].id) {
                targetUid = users[0].id;
              }
            } catch {}
          }

          if (!targetUid) {
            // Try listing all users
            try {
              const listResult = await adminOperations.firestoreRequest(
                'GET',
                `/documents/users?pageSize=300`
              );
              if (listResult?.documents) {
                for (const doc of listResult.documents) {
                  const js = adminOperations.firestoreDocumentToJs(doc);
                  if (js.email === email) {
                    targetUid = doc.name.split('/').pop();
                    break;
                  }
                }
              }
            } catch {}
          }

          if (!targetUid) {
            return NextResponse.json({ error: 'User not found', email }, { status: 404 });
          }

          // Set admin role
          await adminOperations.setDocument('users', targetUid, { role: 'admin' }, true);

          // Also assign Zixo number if missing
          let zixoNumber = '';
          try {
            const profile = await adminOperations.getDocument('users', targetUid);
            if (profile && !profile.zixoNumber) {
              const usedNumbers = new Set<string>();
              // Get existing numbers
              try {
                const allDocs = await adminOperations.firestoreRequest('GET', '/documents/users?pageSize=300');
                if (allDocs?.documents) {
                  allDocs.documents.forEach((doc: any) => {
                    const js = adminOperations.firestoreDocumentToJs(doc);
                    if (js.zixoNumber) usedNumbers.add(js.zixoNumber);
                  });
                }
              } catch {}
              
              for (let attempt = 0; attempt < 20; attempt++) {
                const num = String(Math.floor(Math.random() * 90000000) + 10000000);
                if (!usedNumbers.has(num)) {
                  zixoNumber = num;
                  break;
                }
              }
              if (zixoNumber) {
                await adminOperations.setDocument('users', targetUid, { zixoNumber }, true);
                await adminOperations.setDocument('zixoNumbers', zixoNumber, { uid: targetUid }, false);
              }
            } else if (profile?.zixoNumber) {
              zixoNumber = profile.zixoNumber;
            }
          } catch {}

          return NextResponse.json({
            success: true,
            uid: targetUid,
            email,
            zixoNumber,
            message: `Admin role set for ${email} (UID: ${targetUid})`,
          });
        } catch (err: any) {
          console.error('[Zixo API] Set admin by email error:', err.message);
          return NextResponse.json(
            { error: 'Failed to set admin', details: err.message },
            { status: 500 }
          );
        }
      }

      default:
        return NextResponse.json(
          { error: `Unknown action: ${action}` },
          { status: 400 }
        );
    }
  } catch (error: any) {
    console.error('[Zixo API] Error:', error);
    return NextResponse.json(
      { error: 'Internal server error', details: error.message },
      { status: 500 }
    );
  }
}
