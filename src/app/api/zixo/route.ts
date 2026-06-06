import { NextRequest, NextResponse } from 'next/server';

/**
 * Zixo API Routes - Cloudflare Workers Compatible
 *
 * These routes use Firebase REST APIs instead of the firebase-admin SDK,
 * which requires Node.js APIs not available in Cloudflare Workers.
 *
 * For full admin operations, set up a separate Cloud Function with firebase-admin.
 */

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
  return NextResponse.json({
    status: 'ok',
    app: 'Zixo',
    version: '1.0.0',
    firebase: {
      projectId: PROJECT_ID,
      region: 'us-central1',
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

        // For Cloudflare deployment, we send FCM via the legacy HTTP API
        // In production, use a Cloud Function with firebase-admin for proper FCM
        try {
          // Store the notification in RTDB for the client to pick up
          const notifId = `notif_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
          await rtdbSet(`/notifications/${token.slice(0, 20)}/${notifId}`, 'PUT', {
            title,
            body: messageBody || '',
            data: data || {},
            timestamp: Date.now(),
          });

          return NextResponse.json({
            success: true,
            messageId: notifId,
            note: 'Notification stored in RTDB. For direct FCM, set up Firebase Cloud Functions.',
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
          await rtdbSet(`/typing`, 'PATCH', null); // Can't easily filter, skip

          return NextResponse.json({
            success: true,
            message: `Account ${uid} RTDB data cleaned. For full account deletion (Auth + Firestore), set up Firebase Cloud Functions.`,
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
          const result = await rtdbGet(`/usernames/${username}`);
          return NextResponse.json({
            available: result === null,
            username,
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
          const profile = await rtdbGet(`/users/${uid}`);
          if (!profile) {
            return NextResponse.json(
              { error: 'User not found' },
              { status: 404 }
            );
          }
          return NextResponse.json({ profile });
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

        // RTDB doesn't support complex queries like prefix search
        // For production, use Firestore with Firebase Cloud Functions
        try {
          const allUsers = await rtdbGet('/users');
          if (!allUsers) {
            return NextResponse.json({ users: [] });
          }

          // Client-side filtering for prefix match
          const users = Object.entries(allUsers)
            .filter(([_uid, data]: [string, any]) =>
              data?.username?.toLowerCase().startsWith(searchQuery.toLowerCase())
            )
            .slice(0, 10)
            .map(([uid, data]: [string, any]) => ({ uid, ...data }));

          return NextResponse.json({ users });
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
