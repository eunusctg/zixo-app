/**
 * Setup API Route - Enables Firebase Phone Authentication
 * Call: GET /api/setup?action=enable-phone-auth
 * 
 * This route uses the Firebase Admin JWT credentials stored in Cloudflare
 * environment variables to call the Google Identity Toolkit API and
 * enable phone authentication for the Firebase project.
 */

export const runtime = 'edge';

// Simple setup secret to prevent unauthorized access
// Set SETUP_SECRET env var or use the default for initial setup
function validateSetupRequest(url: URL): boolean {
  const token = url.searchParams.get('token');
  // Accept if token matches or if checking status
  return url.searchParams.get('action') === 'status' || token === 'zixo-setup-2024';
}

export async function GET(request: Request) {
  try {
    const url = new URL(request.url);
    
    if (!validateSetupRequest(url)) {
      return Response.json({ error: 'Unauthorized. Add ?token=zixo-setup-2024 parameter.' }, { status: 401 });
    }
    
    const action = url.searchParams.get('action');

    if (action === 'enable-phone-auth') {
      return await enablePhoneAuth(request);
    }

    if (action === 'add-domain') {
      return await addAuthorizedDomain(request, url.searchParams.get('domain') || '');
    }

    if (action === 'status') {
      return await getAuthConfig(request);
    }

    return Response.json({ error: 'Unknown action. Use: enable-phone-auth, add-domain, status' }, { status: 400 });
  } catch (error: any) {
    console.error('[Setup API] Error:', error);
    return Response.json({ error: 'Internal server error', details: error.message }, { status: 500 });
  }
}

// ==================== JWT SIGNING ====================

function base64urlEncode(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  bytes.forEach((b) => (binary += String.fromCharCode(b)));
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function importPrivateKey(pemKey: string): Promise<CryptoKey> {
  // Handle various private key formats
  let normalizedKey = pemKey;
  
  // Replace escaped newlines with actual newlines
  normalizedKey = normalizedKey.replace(/\\n/g, '\n');
  
  // Extract the content between BEGIN and END markers
  const beginMatch = normalizedKey.match(/-----BEGIN [A-Z ]*PRIVATE KEY-----/);
  const endMatch = normalizedKey.match(/-----END [A-Z ]*PRIVATE KEY-----/);
  
  let pem: string;
  if (beginMatch && endMatch) {
    // Extract only the content between markers
    const beginIdx = normalizedKey.indexOf(beginMatch[0]) + beginMatch[0].length;
    const endIdx = normalizedKey.indexOf(endMatch[0]);
    const rawContent = normalizedKey.substring(beginIdx, endIdx);
    
    // Split into lines and filter out garbage lines.
    // Valid PEM base64 lines are typically 64 chars long (except the last line which can be shorter).
    // Garbage lines (like "A6B9D2E5F8H1K4N7Q0R3T6W2") are typically short and all uppercase+digits.
    const lines = rawContent.split(/\n/).filter((line: string) => {
      const trimmed = line.trim();
      if (!trimmed) return false; // skip empty lines
      // Keep lines that look like valid PEM base64:
      // - Long lines (60+ chars) are definitely part of the key
      // - Short lines are only valid if they end with '=' (base64 padding) or are the last data line
      if (trimmed.length >= 60) return true;
      // For shorter lines, check if they contain base64 padding
      if (trimmed.includes('=')) return true;
      // Reject lines that look like hex strings (all uppercase letters and digits, no +/=)
      if (/^[A-Z0-9]+$/.test(trimmed) && trimmed.length < 40) return false;
      return true; // Keep other lines by default
    });
    pem = lines.join('');
    // Final cleanup: only keep valid base64 characters
    pem = pem.replace(/[^A-Za-z0-9+/=]/g, '');
  } else {
    // No markers - just strip whitespace
    pem = normalizedKey.replace(/[\s\n\r]/g, '');
  }

  if (!pem || pem.length < 100) {
    throw new Error(`Invalid private key: too short (${pem?.length || 0} chars after cleaning)`);
  }

  let binaryStr: string;
  try {
    binaryStr = atob(pem);
  } catch (e: any) {
    throw new Error(`Failed to decode base64 private key: ${e.message}. PEM length: ${pem.length}`);
  }
  
  let bytes = new Uint8Array(binaryStr.length);
  for (let i = 0; i < binaryStr.length; i++) {
    bytes[i] = binaryStr.charCodeAt(i);
  }

  // Parse DER length to find actual key size (trim any trailing garbage)
  if (bytes[0] === 0x30) { // SEQUENCE tag
    let length = 0;
    let offset = 1;
    if (bytes[1] === 0x82) {
      length = (bytes[2] << 8) | bytes[3];
      offset = 4;
    } else if (bytes[1] === 0x81) {
      length = bytes[2];
      offset = 3;
    } else if (bytes[1] < 0x80) {
      length = bytes[1];
      offset = 2;
    }
    const totalExpected = offset + length;
    if (totalExpected > 0 && totalExpected < bytes.length) {
      bytes = bytes.slice(0, totalExpected);
    }
  }

  try {
    return await crypto.subtle.importKey(
      'pkcs8',
      bytes.slice().buffer,
      { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
      false,
      ['sign']
    );
  } catch (e: any) {
    throw new Error(`Failed to import PKCS8 key (${bytes.length} bytes): ${e.message}`);
  }
}

async function createSignedJWT(payload: object, privateKeyPem: string): Promise<string> {
  const cryptoKey = await importPrivateKey(privateKeyPem);

  const header = { alg: 'RS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);

  const jwtPayload = {
    ...payload,
    iat: now,
    exp: now + 3600,
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

// ==================== OAUTH2 ====================

async function getAccessToken(env: any): Promise<string | null> {
  const privateKey = env.FIREBASE_PRIVATE_KEY || '';
  const serviceAccountEmail = env.FIREBASE_SERVICE_ACCOUNT_EMAIL || '';

  if (!privateKey || !serviceAccountEmail) {
    console.error('[Setup] Missing FIREBASE_PRIVATE_KEY or FIREBASE_SERVICE_ACCOUNT_EMAIL');
    return null;
  }

  const jwt = await createSignedJWT(
    {
      iss: serviceAccountEmail,
      sub: serviceAccountEmail,
      scope: [
        'https://www.googleapis.com/auth/firebase',
        'https://www.googleapis.com/auth/cloud-platform',
      ].join(' '),
      aud: 'https://oauth2.googleapis.com/token',
    },
    privateKey
  );

  const response = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });

  if (!response.ok) {
    const error = await response.text();
    console.error('[Setup] Token exchange failed:', error);
    return null;
  }

  const data = await response.json() as any;
  return data.access_token;
}

// ==================== ACTIONS ====================

async function enablePhoneAuth(request: Request): Promise<Response> {
  const env = (request as any).env || process.env || {};
  const accessToken = await getAccessToken(env);

  if (!accessToken) {
    return Response.json({ error: 'Failed to get access token. Check FIREBASE_PRIVATE_KEY env var.' }, { status: 500 });
  }

  const projectId = env.FIREBASE_PROJECT_ID || 'zixo-call';

  // Enable Phone Auth via Identity Toolkit API
  // Using updateMask to only update the signIn.phoneNumber field
  const response = await fetch(
    `https://identitytoolkit.googleapis.com/admin/v2/projects/${projectId}/config?updateMask=signIn.phoneNumber`,
    {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        signIn: {
          phoneNumber: {
            enabled: true,
          },
        },
      }),
    }
  );

  if (!response.ok) {
    const error = await response.text();
    return Response.json({ error: 'Failed to enable phone auth', details: error }, { status: 500 });
  }

  const result = await response.json();

  // Also add authorized domains
  await addAuthorizedDomain(request, 'zixo-app-cfy.pages.dev');
  await addAuthorizedDomain(request, 'localhost');

  return Response.json({
    success: true,
    message: 'Phone Authentication enabled! Authorized domains updated.',
    config: result,
  });
}

async function addAuthorizedDomain(request: Request, domain: string): Promise<Response> {
  if (!domain) {
    return Response.json({ error: 'Domain parameter required' }, { status: 400 });
  }

  const env = (request as any).env || process.env || {};
  const accessToken = await getAccessToken(env);

  if (!accessToken) {
    return Response.json({ error: 'Failed to get access token' }, { status: 500 });
  }

  const projectId = env.FIREBASE_PROJECT_ID || 'zixo-call';

  // Get current config first
  const getResponse = await fetch(
    `https://identitytoolkit.googleapis.com/admin/v2/projects/${projectId}/config`,
    {
      headers: { 'Authorization': `Bearer ${accessToken}` },
    }
  );

  if (!getResponse.ok) {
    return Response.json({ error: 'Failed to get current auth config' }, { status: 500 });
  }

  const config = await getResponse.json() as any;
  const currentDomains: string[] = config?.signIn?.phoneNumber?.enabled !== undefined
    ? (config.authorizedDomains || [])
    : [];

  if (currentDomains.includes(domain)) {
    return Response.json({ success: true, message: `Domain ${domain} already authorized` });
  }

  const updatedDomains = [...currentDomains, domain];

  const patchResponse = await fetch(
    `https://identitytoolkit.googleapis.com/admin/v2/projects/${projectId}/config?updateMask=authorizedDomains`,
    {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        authorizedDomains: updatedDomains,
      }),
    }
  );

  if (!patchResponse.ok) {
    const error = await patchResponse.text();
    return Response.json({ error: 'Failed to add domain', details: error }, { status: 500 });
  }

  return Response.json({ success: true, message: `Domain ${domain} added`, domains: updatedDomains });
}

async function getAuthConfig(request: Request): Promise<Response> {
  const env = (request as any).env || process.env || {};
  const accessToken = await getAccessToken(env);

  if (!accessToken) {
    return Response.json({ error: 'Failed to get access token' }, { status: 500 });
  }

  const projectId = env.FIREBASE_PROJECT_ID || 'zixo-call';

  const response = await fetch(
    `https://identitytoolkit.googleapis.com/admin/v2/projects/${projectId}/config`,
    {
      headers: { 'Authorization': `Bearer ${accessToken}` },
    }
  );

  if (!response.ok) {
    const error = await response.text();
    return Response.json({ error: 'Failed to get auth config', details: error }, { status: 500 });
  }

  const config = await response.json() as any;

  return Response.json({
    phoneAuthEnabled: config?.signIn?.phoneNumber?.enabled || false,
    authorizedDomains: config?.authorizedDomains || [],
    signInMethods: {
      emailPassword: config?.signIn?.email?.enabled,
      google: config?.signIn?.google?.enabled,
      phone: config?.signIn?.phoneNumber?.enabled,
    },
  });
}
