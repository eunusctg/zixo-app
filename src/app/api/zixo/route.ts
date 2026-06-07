import { NextRequest, NextResponse } from 'next/server';

// runtime = 'edge' (Cloudflare Pages runs all routes on the edge by default)

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
          const requesterProfile = await adminOperations.getDocument('users', requesterUid);
          if (!requesterProfile || requesterProfile.role !== 'admin') {
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
          const requesterProfile = await adminOperations.getDocument('users', requesterUid);
          if (!requesterProfile || requesterProfile.role !== 'admin') {
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
          const requesterProfile = await adminOperations.getDocument('users', requesterUid);
          if (!requesterProfile || requesterProfile.role !== 'admin') {
            return NextResponse.json(
              { error: 'Unauthorized: Only admins can list all users' },
              { status: 403 }
            );
          }

          // List all users from Firestore (using admin API which bypasses rules)
          // Use listDocuments instead of query to avoid needing indexes
          let users: any[] = [];
          try {
            const listResult = await adminOperations.firestoreRequest(
              'GET',
              `/documents/users?pageSize=100`
            );
            if (listResult?.documents) {
              users = listResult.documents.map((doc: any) => ({
                id: doc.name.split('/').pop(),
                ...adminOperations.firestoreDocumentToJs(doc),
              }));
            }
          } catch (listErr: any) {
            console.warn('[Zixo API] List documents failed, trying query:', listErr.message);
            // Fallback to query
            try {
              users = await adminOperations.queryCollection('users', 'username', 'IS_NOT_NULL', '');
            } catch (queryErr: any) {
              console.warn('[Zixo API] Query also failed:', queryErr.message);
            }
          }
          return NextResponse.json({
            success: true,
            users: users || [],
            count: users?.length || 0,
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
          const requesterProfile = await adminOperations.getDocument('users', requesterUid);
          if (!requesterProfile || requesterProfile.role !== 'admin') {
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
          const requesterProfile = await adminOperations.getDocument('users', requesterUid);
          if (!requesterProfile || requesterProfile.role !== 'admin') {
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
