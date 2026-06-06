/**
 * Firebase Admin SDK - Server-side only
 * Used in API routes for privileged operations that client SDK can't do
 *
 * IMPORTANT: This file must NEVER be imported in client-side code
 */

import * as admin from 'firebase-admin';

// ==================== ADMIN INITIALIZATION ====================

// Prevent re-initialization on HMR
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: 'zixo-call',
      clientEmail: 'firebase-adminsdk-fbsvc@zixo-call.iam.gserviceaccount.com',
      // The private key needs to be obtained from Firebase Console > Service Accounts
      // For now, we use the database secret for RTDB operations
      // In production, store the full service account JSON in FIREBASE_SERVICE_ACCOUNT env var
      privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
    } as any),
    databaseURL: 'https://zixo-call-default-rtdb.firebaseio.com',
    storageBucket: 'zixo-call.firebasestorage.app',
  });
}

// ==================== ADMIN SERVICES ====================

export const adminAuth = admin.auth();
export const adminDb = admin.firestore();
export const adminRtdb = admin.database();
export const adminStorage = admin.storage();

export default admin;
