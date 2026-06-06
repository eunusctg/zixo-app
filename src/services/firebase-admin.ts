/**
 * Firebase Admin Operations - Cloudflare Workers Compatible
 *
 * Uses Firebase REST APIs with JWT-based OAuth2 authentication
 * instead of the firebase-admin SDK (which requires Node.js).
 *
 * This implementation creates signed JWTs using the Web Crypto API
 * to obtain Google OAuth2 access tokens for Firestore and FCM operations.
 */

const PROJECT_ID = 'zixo-call';
const SERVICE_ACCOUNT_EMAIL = 'firebase-adminsdk-fbsvc@zixo-call.iam.gserviceaccount.com';

// The private key will be set via environment variable
// Format: "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
function getPrivateKey(): string {
  return process.env.FIREBASE_PRIVATE_KEY || '';
}

// ==================== JWT SIGNING (Web Crypto API) ====================

/**
 * Base64url encode a buffer (no padding)
 */
function base64urlEncode(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  bytes.forEach((b) => (binary += String.fromCharCode(b)));
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/**
 * Import a PEM-formatted private key for use with Web Crypto API
 */
async function importPrivateKey(pemKey: string): Promise<CryptoKey> {
  // Clean up the PEM key
  const pem = pemKey
    .replace(/-----BEGIN PRIVATE KEY-----/g, '')
    .replace(/-----END PRIVATE KEY-----/g, '')
    .replace(/-----BEGIN RSA PRIVATE KEY-----/g, '')
    .replace(/-----END RSA PRIVATE KEY-----/g, '')
    .replace(/\s/g, '');

  const binaryStr = atob(pem);
  const bytes = new Uint8Array(binaryStr.length);
  for (let i = 0; i < binaryStr.length; i++) {
    bytes[i] = binaryStr.charCodeAt(i);
  }

  return crypto.subtle.importKey(
    'pkcs8',
    bytes.buffer,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['sign']
  );
}

/**
 * Create a signed JWT for Google OAuth2
 */
async function createSignedJWT(payload: object, privateKeyPem: string): Promise<string> {
  const cryptoKey = await importPrivateKey(privateKeyPem);

  const header = { alg: 'RS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);

  const jwtPayload = {
    ...payload,
    iss: SERVICE_ACCOUNT_EMAIL,
    sub: SERVICE_ACCOUNT_EMAIL,
    iat: now,
    exp: now + 3600, // 1 hour
  };

  const headerB64 = base64urlEncode(new TextEncoder().encode(JSON.stringify(header)));
  const payloadB64 = base64urlEncode(new TextEncoder().encode(JSON.stringify(jwtPayload)));
  const signInput = `${headerB64}.${payloadB64}`;

  const signature = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    cryptoKey,
    new TextEncoder().encode(signInput)
  );

  const signatureB64 = base64urlEncode(signature);
  return `${signInput}.${signatureB64}`;
}

// ==================== OAUTH2 ACCESS TOKEN ====================

// Cache the access token
let cachedToken: { token: string; expiresAt: number } | null = null;

/**
 * Get a Google OAuth2 access token using the service account
 */
async function getAccessToken(): Promise<string | null> {
  const privateKey = getPrivateKey();
  if (!privateKey) {
    console.warn('[Firebase Admin] No private key configured. Set FIREBASE_PRIVATE_KEY env var.');
    return null;
  }

  // Return cached token if still valid
  if (cachedToken && cachedToken.expiresAt > Date.now() - 60000) {
    return cachedToken.token;
  }

  try {
    // Create the JWT assertion
    const jwt = await createSignedJWT(
      {
        scope: [
          'https://www.googleapis.com/auth/datastore',
          'https://www.googleapis.com/auth/firebase.messaging',
          'https://www.googleapis.com/auth/firebase',
          'https://www.googleapis.com/auth/cloud-platform',
        ].join(' '),
        aud: 'https://oauth2.googleapis.com/token',
      },
      privateKey
    );

    // Exchange JWT for access token
    const response = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
    });

    if (!response.ok) {
      const error = await response.text();
      console.error('[Firebase Admin] Token exchange failed:', error);
      return null;
    }

    const data = await response.json();
    cachedToken = {
      token: data.access_token,
      expiresAt: Date.now() + (data.expires_in || 3600) * 1000,
    };

    return data.access_token;
  } catch (error) {
    console.error('[Firebase Admin] Failed to get access token:', error);
    return null;
  }
}

// ==================== FIRESTORE REST API ====================

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
    throw new Error('No admin access token. Set FIREBASE_PRIVATE_KEY environment variable.');
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
    const error = await response.json().catch(() => ({ error: { message: 'Unknown error' } }));
    throw new Error(error.error?.message || `Firestore request failed: ${response.status}`);
  }

  // Some methods return empty responses
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

// ==================== RTDB REST API ====================

const DATABASE_SECRET = process.env.FIREBASE_DATABASE_SECRET || '';

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

// ==================== FCM REST API (v1) ====================

/**
 * Send an FCM notification using the Firebase Cloud Messaging v1 API
 * Requires the OAuth2 access token obtained via the service account
 */
async function sendFCMMessage(
  fcmToken: string,
  notification: { title: string; body: string },
  data?: Record<string, string>
): Promise<string> {
  const accessToken = await getAccessToken();

  if (accessToken) {
    // Use FCM v1 API with OAuth2 token
    const url = `https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`;

    const message: any = {
      token: fcmToken,
      notification,
      android: {
        priority: 'high' as const,
        notification: {
          sound: 'default',
          channel_id: 'zixo_messages',
        },
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            badge: 1,
          },
        },
      },
    };

    if (data) {
      message.data = data;
    }

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ message }),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: { message: 'Unknown FCM error' } }));
      throw new Error(error.error?.message || `FCM send failed: ${response.status}`);
    }

    const result = await response.json();
    return result.name;
  }

  // Fallback: Store notification in RTDB for client polling
  const notifId = `notif_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  await rtdbRequest('PUT', `/notifications/${fcmToken.slice(0, 20)}/${notifId}`, {
    title: notification.title,
    body: notification.body,
    data: data || {},
    timestamp: Date.now(),
  });

  return notifId;
}

// ==================== FIRESTORE HELPERS ====================

/**
 * Get a Firestore document
 */
async function getDocument(collection: string, docId: string): Promise<any> {
  const result = await firestoreRequest('GET', `/documents/${collection}/${docId}`);
  return result?.fields ? firestoreDocumentToJs(result) : null;
}

/**
 * Create or update a Firestore document
 */
async function setDocument(
  collection: string,
  docId: string,
  data: any,
  merge: boolean = false
): Promise<any> {
  const path = `/documents/${collection}/${docId}${merge ? '?currentDocument.exists=true&updateMask.fieldPaths=' + Object.keys(data).join('&updateMask.fieldPaths=') : ''}`;
  return firestoreRequest('PATCH', path, jsToFirestoreDocument(data));
}

/**
 * Query Firestore documents
 */
async function queryCollection(
  collection: string,
  fieldPath: string,
  op: string,
  value: any
): Promise<any[]> {
  const body = {
    structuredQuery: {
      from: [{ collectionId: collection }],
      where: {
        fieldFilter: {
          field: { fieldPath },
          op,
          value: jsToFirestoreValue(value),
        },
      },
      limit: 20,
    },
  };

  const result = await firestoreRequest('POST', '/documents:runQuery', body);
  if (!Array.isArray(result)) return [];

  return result
    .filter((item: any) => item.document)
    .map((item: any) => ({
      id: item.document.name.split('/').pop(),
      ...firestoreDocumentToJs(item.document),
    }));
}

/**
 * Delete a Firestore document
 */
async function deleteDocument(collection: string, docId: string): Promise<void> {
  await firestoreRequest('DELETE', `/documents/${collection}/${docId}`);
}

// ==================== FIRESTORE VALUE CONVERSION ====================

/**
 * Convert a Firestore document to a plain JS object
 */
function firestoreDocumentToJs(doc: any): any {
  if (!doc.fields) return {};
  const result: any = {};
  for (const [key, value] of Object.entries(doc.fields)) {
    result[key] = firestoreValueToJs(value as any);
  }
  return result;
}

/**
 * Convert a Firestore value to a JS value
 */
function firestoreValueToJs(value: any): any {
  if (value.stringValue !== undefined) return value.stringValue;
  if (value.integerValue !== undefined) return parseInt(value.integerValue, 10);
  if (value.doubleValue !== undefined) return parseFloat(value.doubleValue);
  if (value.booleanValue !== undefined) return value.booleanValue;
  if (value.nullValue !== undefined) return null;
  if (value.timestampValue !== undefined) return new Date(value.timestampValue);
  if (value.arrayValue?.values) return value.arrayValue.values.map(firestoreValueToJs);
  if (value.mapValue?.fields) return firestoreDocumentToJs(value.mapValue);
  return value;
}

/**
 * Convert a JS object to a Firestore document
 */
function jsToFirestoreDocument(data: any): { fields: any } {
  const fields: any = {};
  for (const [key, value] of Object.entries(data)) {
    fields[key] = jsToFirestoreValue(value);
  }
  return { fields };
}

/**
 * Convert a JS value to a Firestore value
 */
function jsToFirestoreValue(value: any): any {
  if (value === null || value === undefined) return { nullValue: null };
  if (typeof value === 'string') return { stringValue: value };
  if (typeof value === 'number') {
    return Number.isInteger(value)
      ? { integerValue: value.toString() }
      : { doubleValue: value };
  }
  if (typeof value === 'boolean') return { booleanValue: value };
  if (value instanceof Date) return { timestampValue: value.toISOString() };
  if (Array.isArray(value)) {
    return {
      arrayValue: {
        values: value.map(jsToFirestoreValue),
      },
    };
  }
  if (typeof value === 'object') {
    return { mapValue: jsToFirestoreDocument(value) };
  }
  return { stringValue: String(value) };
}

// ==================== VERIFIED CUSTOM TOKEN ====================

/**
 * Create a Firebase custom auth token
 * This allows the server to mint tokens for custom authentication flows
 */
async function createCustomToken(uid: string, claims?: Record<string, any>): Promise<string> {
  const privateKey = getPrivateKey();
  if (!privateKey) {
    throw new Error('FIREBASE_PRIVATE_KEY not configured');
  }

  const now = Math.floor(Date.now() / 1000);
  const payload: any = {
    iss: SERVICE_ACCOUNT_EMAIL,
    sub: SERVICE_ACCOUNT_EMAIL,
    aud: 'https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit',
    uid,
    iat: now,
    exp: now + 3600,
  };

  if (claims) {
    payload.claims = claims;
  }

  return createSignedJWT(payload, privateKey);
}

// ==================== EXPORTED OPERATIONS ====================

export const adminOperations = {
  // RTDB operations (work with database secret)
  rtdbRead: rtdbRequest.bind(null, 'GET'),
  rtdbWrite: rtdbRequest.bind(null, 'PUT'),
  rtdbUpdate: rtdbRequest.bind(null, 'PATCH'),
  rtdbDelete: rtdbRequest.bind(null, 'DELETE'),

  // Firestore operations (require OAuth2 via service account)
  getDocument,
  setDocument,
  queryCollection,
  deleteDocument,
  firestoreRequest,

  // FCM operations
  sendFCMMessage,

  // Custom auth token
  createCustomToken,

  // Token access
  getAccessToken,
};
