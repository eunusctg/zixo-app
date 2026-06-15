import { NextRequest, NextResponse } from 'next/server';

export const runtime = 'edge';

/**
 * Zixo Passkey Registration Challenge Endpoint
 *
 * Generates a WebAuthn PublicKeyCredentialCreationOptions JSON payload
 * with strict domain compliance for the Relying Party ID.
 *
 * CRITICAL RULES:
 * 1. rp.id MUST be exactly "zixo.pages.dev" — no protocol, no trailing slash
 * 2. rp.name MUST be "Zixo" — hardcoded brand name
 * 3. user.id MUST be URL-safe Base64 encoded (no padding) for Android byte array mapping
 * 4. challenge MUST be URL-safe Base64 encoded (no padding) — 32 bytes of entropy
 */

// ── URL-safe Base64 encoding without padding ──
function toBase64Url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

// ── Convert string to URL-safe Base64 without padding ──
function stringToBase64Url(str: string): string {
  const encoder = new TextEncoder();
  const data = encoder.encode(str);
  let binary = '';
  for (let i = 0; i < data.length; i++) {
    binary += String.fromCharCode(data[i]);
  }
  return btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

// ── Validate Relying Party ID format ──
function validateRpId(rpId: string): boolean {
  if (rpId.includes('://') || rpId.includes('/') || rpId.includes(':')) {
    return false;
  }
  return /^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$/.test(rpId);
}

const RP_ID = 'zixo.pages.dev';
const RP_NAME = 'Zixo';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { uid, email, displayName } = body;

    if (!uid) {
      return NextResponse.json(
        { error: 'Missing required field: uid' },
        { status: 400 }
      );
    }

    // ── Validate RP ID at generation time ──
    if (!validateRpId(RP_ID)) {
      console.error('[Passkey] CRITICAL: RP ID validation failed:', RP_ID);
      return NextResponse.json(
        { error: 'Server configuration error: invalid RP ID' },
        { status: 500 }
      );
    }

    // ── Generate cryptographically secure challenge (32 bytes) ──
    const challengeBuffer = new ArrayBuffer(32);
    const challengeView = new Uint8Array(challengeBuffer);
    crypto.getRandomValues(challengeView);
    const challenge = toBase64Url(challengeBuffer);

    // ── Encode user.id as URL-safe Base64 (no padding) ──
    const userIdBase64Url = stringToBase64Url(uid);

    // ── Construct the PublicKeyCredentialCreationOptions ──
    const creationOptions = {
      challenge: challenge,
      rp: {
        name: RP_NAME,
        id: RP_ID
      },
      user: {
        id: userIdBase64Url,
        name: email || `${uid}@zixo.pages.dev`,
        displayName: displayName || 'Zixo User'
      },
      pubKeyCredParams: [
        { type: 'public-key', alg: -7 },
        { type: 'public-key', alg: -257 }
      ],
      timeout: 180000,
      excludeCredentials: [],
      authenticatorSelection: {
        authenticatorAttachment: 'platform',
        requireResidentKey: true,
        residentKey: 'required',
        userVerification: 'required'
      },
      attestation: 'none'
    };

    // ── Store challenge in RTDB for later verification ──
    const PROJECT_ID = 'zixo-call';
    const RTDB_SECRET = process.env.FIREBASE_DATABASE_SECRET || '';

    if (RTDB_SECRET) {
      try {
        const challengeData = {
          challenge: challenge,
          uid: uid,
          createdAt: Date.now(),
          expiresAt: Date.now() + 180000
        };
        await fetch(
          `https://${PROJECT_ID}-default-rtdb.firebaseio.com/webauthn/challenges/${uid.replace(/[^a-zA-Z0-9]/g, '_')}.json?auth=${RTDB_SECRET}`,
          {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(challengeData)
          }
        );
      } catch (err: any) {
        console.warn('[Passkey] Failed to store challenge in RTDB:', err.message);
      }
    }

    console.log('[Passkey] Challenge generated for uid:', uid, 'rp.id:', RP_ID);

    return NextResponse.json(creationOptions);
  } catch (error: any) {
    console.error('[Passkey] Challenge generation error:', error);
    return NextResponse.json(
      { error: 'Failed to generate passkey challenge', details: error.message },
      { status: 500 }
    );
  }
}

export async function GET() {
  return NextResponse.json({
    endpoint: 'passkey-challenge',
    method: 'POST',
    requiredFields: ['uid'],
    optionalFields: ['email', 'displayName'],
    rpId: RP_ID,
    rpName: RP_NAME
  });
}
