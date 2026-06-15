import { NextResponse } from 'next/server';

export const runtime = 'edge';

/**
 * Digital Asset Links — assetlinks.json Endpoint
 *
 * CRITICAL: This file MUST be publicly accessible at:
 * https://zixo.pages.dev/.well-known/assetlinks.json
 *
 * Android's CredentialManager uses Digital Asset Links to verify
 * that the app (com.zixo.app) is authorized to create WebAuthn
 * passkeys for the domain zixo.pages.dev.
 *
 * Without this endpoint returning 200 with valid JSON, passkey
 * registration will fail with: "rp id cannot be validated"
 *
 * SHA-256 fingerprint from release keystore (zixo-release.jks, alias: zixo):
 * 05:2C:95:BD:FB:FE:9C:12:92:02:CA:B4:F8:F6:F6:6B:37:25:B3:94:18:76:63:A3:D6:7E:C6:F6:4A:95:49:7B
 */

const ASSET_LINKS = [
  {
    relation: [
      'delegate_permission/common.handle_all_urls',
      'delegate_permission/common.get_login_creds'
    ],
    target: {
      namespace: 'android_app',
      package_name: 'com.zixo.app',
      sha256_cert_fingerprints: [
        '05:2C:95:BD:FB:FE:9C:12:92:02:CA:B4:F8:F6:F6:6B:37:25:B3:94:18:76:63:A3:D6:7E:C6:F6:4A:95:49:7B'
      ]
    }
  }
];

export async function GET() {
  return new NextResponse(JSON.stringify(ASSET_LINKS), {
    status: 200,
    headers: {
      'Content-Type': 'application/json',
      'Cache-Control': 'public, max-age=86400, immutable',
      'Access-Control-Allow-Origin': '*',
      'X-Content-Type-Options': 'nosniff'
    }
  });
}
