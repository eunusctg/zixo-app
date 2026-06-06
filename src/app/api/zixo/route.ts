import { NextRequest, NextResponse } from 'next/server';

/**
 * Zixo API Routes
 * Server-side Firebase operations using Admin SDK
 */

// ==================== HEALTH CHECK ====================
export async function GET() {
  return NextResponse.json({
    status: 'ok',
    app: 'Zixo',
    version: '1.0.0',
    firebase: {
      projectId: 'zixo-call',
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
      // ==================== NOTIFICATIONS ====================
      case 'sendNotification': {
        const { token, title, body: messageBody, data } = body;

        if (!token || !title) {
          return NextResponse.json(
            { error: 'Missing required fields: token, title' },
            { status: 400 }
          );
        }

        try {
          const admin = await import('@/services/firebase-admin');
          const { getMessaging } = await import('firebase-admin/messaging');

          const message = {
            token,
            notification: {
              title,
              body: messageBody || '',
            },
            data: data || {},
            webpush: {
              notification: {
                icon: '/logo.svg',
                badge: '/logo.svg',
                tag: data?.chatId || 'zixo-notification',
                requireInteraction: data?.type === 'call',
                actions: data?.type === 'call'
                  ? [{ action: 'answer', title: 'Answer' }, { action: 'decline', title: 'Decline' }]
                  : [{ action: 'reply', title: 'Reply' }, { action: 'mark_read', title: 'Mark as Read' }],
              },
            },
            android: {
              priority: 'high' as const,
              notification: {
                channelId: data?.type === 'call' ? 'zixo-calls' : 'zixo-messages',
                priority: 'high' as const,
              },
            },
          };

          const response = await getMessaging().send(message);
          return NextResponse.json({ success: true, messageId: response });
        } catch (err: any) {
          console.error('[Zixo API] FCM send error:', err.message);

          // If token is invalid, return specific error
          if (err.code === 'messaging/invalid-registration-token' ||
              err.code === 'messaging/registration-token-not-registered') {
            return NextResponse.json(
              { error: 'Invalid FCM token', code: err.code },
              { status: 400 }
            );
          }

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
          const admin = await import('@/services/firebase-admin');

          // Delete user from Firebase Auth
          await admin.adminAuth.deleteUser(uid);

          // Delete user profile from Firestore
          await admin.adminDb.collection('users').doc(uid).delete();

          // Delete username mapping
          const usernameSnap = await admin.adminDb
            .collection('usernames')
            .where('uid', '==', uid)
            .get();
          const batch = admin.adminDb.batch();
          usernameSnap.docs.forEach((doc) => batch.delete(doc.ref));

          // Delete user's presence in RTDB
          await admin.adminRtdb.ref(`presence/${uid}`).remove();

          // Delete all chat memberships where user is a participant
          const chatsSnap = await admin.adminDb
            .collection('chats')
            .where('participants', 'array-contains', uid)
            .get();

          chatsSnap.docs.forEach((chatDoc) => {
            const chatData = chatDoc.data();
            // For 1-on-1 chats, delete the whole chat
            if (!chatData.isGroup) {
              batch.delete(chatDoc.ref);
              // Also delete all messages in the chat
              admin.adminDb.collection('chats').doc(chatDoc.id).collection('messages')
                .get().then((msgsSnap) => {
                  const msgBatch = admin.adminDb.batch();
                  msgsSnap.docs.forEach((msgDoc) => msgBatch.delete(msgDoc.ref));
                  msgBatch.commit();
                });
            } else {
              // For group chats, remove the user from participants
              batch.update(chatDoc.ref, {
                participants: admin.FieldValue.arrayRemove(uid),
                [`unreadCount.${uid}`]: admin.FieldValue.delete(),
              });
            }
          });

          // Delete call records involving the user
          const callerCallsSnap = await admin.adminDb
            .collection('calls')
            .where('callerId', '==', uid)
            .get();
          callerCallsSnap.docs.forEach((doc) => batch.delete(doc.ref));

          const receiverCallsSnap = await admin.adminDb
            .collection('calls')
            .where('receiverId', '==', uid)
            .get();
          receiverCallsSnap.docs.forEach((doc) => batch.delete(doc.ref));

          await batch.commit();

          return NextResponse.json({
            success: true,
            message: `Account ${uid} and all associated data deleted`,
          });
        } catch (err: any) {
          console.error('[Zixo API] Account deletion error:', err.message);
          return NextResponse.json(
            { error: 'Failed to delete account', details: err.message },
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
          const admin = await import('@/services/firebase-admin');
          const doc = await admin.adminDb.collection('usernames').doc(username).get();

          return NextResponse.json({
            available: !doc.exists,
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
          const admin = await import('@/services/firebase-admin');
          const doc = await admin.adminDb.collection('users').doc(uid).get();

          if (!doc.exists) {
            return NextResponse.json(
              { error: 'User not found' },
              { status: 404 }
            );
          }

          return NextResponse.json({ profile: doc.data() });
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
        const { query: searchQuery, limit: searchLimit = 10 } = body;

        if (!searchQuery) {
          return NextResponse.json(
            { error: 'Missing required field: query' },
            { status: 400 }
          );
        }

        try {
          const admin = await import('@/services/firebase-admin');

          // Search by username prefix
          const snapshot = await admin.adminDb
            .collection('users')
            .where('username', '>=', searchQuery)
            .where('username', '<=', searchQuery + '\uf8ff')
            .limit(searchLimit)
            .get();

          const users = snapshot.docs.map((doc) => ({
            uid: doc.id,
            ...doc.data(),
          }));

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
          const admin = await import('@/services/firebase-admin');

          // Remove call signals older than 5 minutes
          const fiveMinutesAgo = Date.now() - 5 * 60 * 1000;
          const staleCallsRef = admin.adminRtdb.ref('calls');
          const snapshot = await staleCallsRef.orderByChild('createdAt').endAt(fiveMinutesAgo).once('value');

          const updates: Record<string, null> = {};
          snapshot.forEach((child) => {
            updates[child.key!] = null;
          });

          if (Object.keys(updates).length > 0) {
            await staleCallsRef.update(updates);
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
