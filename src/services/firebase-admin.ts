/**
 * Firebase Admin Operations - Cloudflare Workers Compatible
 *
 * Uses Firebase REST APIs instead of firebase-admin SDK
 * because firebase-admin requires Node.js APIs not available in Workers.
 *
 * For full admin operations (deleteAccount, etc.), use a separate
 * Cloudflare Worker or Cloud Function with firebase-admin.
 */

const PROJECT_ID = 'zixo-call';
const DATABASE_SECRET = process.env.FIREBASE_DATABASE_SECRET || '';

// ==================== FIRESTORE REST API ====================

/**
 * Get a Google OAuth2 access token using the service account
 * This is used for Firestore REST API authentication
 */
async function getAccessToken(): Promise<string | null> {
  // In Cloudflare Workers, we'd need to use the service account JWT flow
  // For now, we'll use the database secret for RTDB operations
  // and rely on client-side Firebase SDK for Firestore operations
  return null;
}

/**
 * Make an authenticated Firestore REST API request
 */
async function firestoreRequest(
  method: string,
  path: string,
  body?: any
): Promise<any> {
  const token = await getAccessToken();
  if (!token) {
    throw new Error('No admin access token available. Server-side Firestore operations require Firebase Admin setup.');
  }

  const url = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)${path}`;

  const options: RequestInit = {
    method,
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(url, options);

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error?.message || 'Firestore request failed');
  }

  return response.json();
}

// ==================== RTDB REST API ====================

/**
 * Make an authenticated RTDB REST API request using the database secret
 */
async function rtdbRequest(
  method: string,
  path: string,
  body?: any
): Promise<any> {
  const url = `https://${PROJECT_ID}-default-rtdb.firebaseio.com${path}.json?auth=${DATABASE_SECRET}`;

  const options: RequestInit = {
    method,
    headers: {
      'Content-Type': 'application/json',
    },
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(url, options);

  if (!response.ok) {
    throw new Error(`RTDB request failed: ${response.status}`);
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

// ==================== FCM REST API ====================

/**
 * Send an FCM notification using the REST API
 */
async function sendFCMMessage(token: string, notification: any, data?: any): Promise<string> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    // Use FCM v1 API with OAuth2 token
    const url = `https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        message: {
          token,
          notification,
          data,
        },
      }),
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error?.message || 'FCM send failed');
    }

    const result = await response.json();
    return result.name;
  }

  // Fallback: Use the legacy FCM API with the server key (not recommended for production)
  throw new Error('FCM requires OAuth2 access token. Set up Firebase Admin credentials.');
}

// ==================== EXPORTED OPERATIONS ====================

export const adminOperations = {
  // RTDB operations (work with database secret)
  rtdbRead: rtdbRequest.bind(null, 'GET'),
  rtdbWrite: rtdbRequest.bind(null, 'PUT'),
  rtdbUpdate: rtdbRequest.bind(null, 'PATCH'),
  rtdbDelete: rtdbRequest.bind(null, 'DELETE'),

  // Firestore operations (require OAuth2)
  firestoreRequest,

  // FCM operations
  sendFCMMessage,
};
