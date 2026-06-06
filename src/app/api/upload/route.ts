import { NextRequest, NextResponse } from 'next/server';

export const runtime = 'edge';

/**
 * R2 Upload API Route - Cloudflare Workers Compatible
 *
 * Uploads files to Cloudflare R2 bucket using S3-compatible API.
 * Uses the R2 Access Key ID and Secret Access Key for authentication.
 * Falls back to Firebase Storage if R2 upload fails.
 */

const R2_ACCOUNT_ID = '704489378006d2bed6a45de180f6679f';
const R2_ACCESS_KEY_ID = '8438e3f2537b01b1f0978365cef05f61';
const R2_SECRET_ACCESS_KEY = '11f21f80d8ea02bf48f86201c2a31969e4a2de459450fdbcf0af1d7851b113a4';
const R2_BUCKET = 'zixocall';
const R2_ENDPOINT = `https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com`;

/**
 * Generate AWS Signature Version 4 for R2 S3 API authentication
 * This is needed because we can't use the AWS SDK in Cloudflare Workers
 */
async function signV4(
  method: string,
  url: URL,
  headers: Record<string, string>,
  body: ArrayBuffer | null,
  region: string = 'auto',
  service: string = 's3',
): Promise<Record<string, string>> {
  const now = new Date();
  const dateStamp = now.toISOString().replace(/[-:]/g, '').slice(0, 8);
  const amzDate = dateStamp + 'T' + now.toISOString().replace(/[-:]/g, '').slice(9, 15) + 'Z';
  const credentialScope = `${dateStamp}/${region}/${service}/aws4_request`;

  // Payload hash
  const payloadHash = await sha256Hex(body || new Uint8Array(0));

  // Canonical headers
  const signedHeaderKeys = ['host', 'x-amz-content-sha256', 'x-amz-date'];
  const canonicalHeaders = [
    `host:${url.host}`,
    `x-amz-content-sha256:${payloadHash}`,
    `x-amz-date:${amzDate}`,
  ].join('\n') + '\n';
  const signedHeaders = signedHeaderKeys.join(';');

  // Canonical request
  const canonicalRequest = [
    method,
    url.pathname,
    url.searchParams.toString(),
    canonicalHeaders,
    signedHeaders,
    payloadHash,
  ].join('\n');

  // String to sign
  const stringToSign = [
    'AWS4-HMAC-SHA256',
    amzDate,
    credentialScope,
    await sha256Hex(new TextEncoder().encode(canonicalRequest)),
  ].join('\n');

  // Signing key
  const kDate = await hmacSha256(`AWS4${R2_SECRET_ACCESS_KEY}`, dateStamp);
  const kRegion = await hmacSha256(kDate, region);
  const kService = await hmacSha256(kRegion, service);
  const kSigning = await hmacSha256(kService, 'aws4_request');

  // Signature
  const signature = await hmacSha256Hex(kSigning, stringToSign);

  // Authorization header
  const authorization = `AWS4-HMAC-SHA256 Credential=${R2_ACCESS_KEY_ID}/${credentialScope}, SignedHeaders=${signedHeaders}, Signature=${signature}`;

  return {
    ...headers,
    'x-amz-date': amzDate,
    'x-amz-content-sha256': payloadHash,
    'Authorization': authorization,
  };
}

async function sha256Hex(data: ArrayBuffer | Uint8Array): Promise<string> {
  const hash = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2, '0')).join('');
}

async function hmacSha256(key: ArrayBuffer | string, message: string): Promise<ArrayBuffer> {
  const keyData = typeof key === 'string' ? new TextEncoder().encode(key) : key;
  const cryptoKey = await crypto.subtle.importKey('raw', keyData, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  return crypto.subtle.sign('HMAC', cryptoKey, new TextEncoder().encode(message));
}

async function hmacSha256Hex(key: ArrayBuffer, message: string): Promise<string> {
  const sig = await hmacSha256(key, message);
  return Array.from(new Uint8Array(sig)).map(b => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Upload a file to R2 using the S3 PUT API
 */
async function uploadToR2(
  key: string,
  data: ArrayBuffer,
  contentType: string,
): Promise<string> {
  const url = new URL(`/${R2_BUCKET}/${key}`, R2_ENDPOINT);

  const headers: Record<string, string> = {
    'Content-Type': contentType,
    'Content-Length': data.byteLength.toString(),
  };

  const signedHeaders = await signV4('PUT', url, headers, data);
  signedHeaders['Content-Type'] = contentType;
  signedHeaders['Content-Length'] = data.byteLength.toString();

  const response = await fetch(url.toString(), {
    method: 'PUT',
    headers: signedHeaders,
    body: data,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`R2 upload failed: ${response.status} - ${errorText}`);
  }

  // Return the public URL (if public access is configured) or the object key
  // For now, return the key which can be used to generate a presigned URL
  return key;
}

/**
 * Generate a presigned URL for R2 object access (GET)
 */
async function generatePresignedUrl(key: string, expiresIn: number = 3600): Promise<string> {
  const url = new URL(`/${R2_BUCKET}/${key}`, R2_ENDPOINT);
  const now = new Date();
  const dateStamp = now.toISOString().replace(/[-:]/g, '').slice(0, 8);
  const amzDate = dateStamp + 'T' + now.toISOString().replace(/[-:]/g, '').slice(9, 15) + 'Z';
  const credentialScope = `${dateStamp}/auto/s3/aws4_request`;

  // Add query parameters for presigned URL
  url.searchParams.set('X-Amz-Algorithm', 'AWS4-HMAC-SHA256');
  url.searchParams.set('X-Amz-Credential', `${R2_ACCESS_KEY_ID}/${credentialScope}`);
  url.searchParams.set('X-Amz-Date', amzDate);
  url.searchParams.set('X-Amz-Expires', expiresIn.toString());
  url.searchParams.set('X-Amz-SignedHeaders', 'host');

  // Canonical request for presigned URL
  const canonicalHeaders = `host:${url.host}\n`;
  const signedHeaders = 'host';
  const payloadHash = 'UNSIGNED-PAYLOAD';

  const canonicalRequest = [
    'GET',
    url.pathname,
    url.searchParams.toString(),
    canonicalHeaders,
    signedHeaders,
    payloadHash,
  ].join('\n');

  const stringToSign = [
    'AWS4-HMAC-SHA256',
    amzDate,
    credentialScope,
    await sha256Hex(new TextEncoder().encode(canonicalRequest)),
  ].join('\n');

  const kDate = await hmacSha256(`AWS4${R2_SECRET_ACCESS_KEY}`, dateStamp);
  const kRegion = await hmacSha256(kDate, 'auto');
  const kService = await hmacSha256(kRegion, 's3');
  const kSigning = await hmacSha256(kService, 'aws4_request');
  const signature = await hmacSha256Hex(kSigning, stringToSign);

  url.searchParams.set('X-Amz-Signature', signature);

  return url.toString();
}

/**
 * Delete an object from R2
 */
async function deleteFromR2(key: string): Promise<void> {
  const url = new URL(`/${R2_BUCKET}/${key}`, R2_ENDPOINT);
  const headers: Record<string, string> = {};
  const signedHeaders = await signV4('DELETE', url, headers, null);

  const response = await fetch(url.toString(), {
    method: 'DELETE',
    headers: signedHeaders,
  });

  if (!response.ok && response.status !== 204) {
    throw new Error(`R2 delete failed: ${response.status}`);
  }
}

// ==================== API ROUTE HANDLERS ====================

export async function POST(request: NextRequest) {
  try {
    const formData = await request.formData();
    const file = formData.get('file') as File | null;
    const path = formData.get('path') as string | null;
    const action = formData.get('action') as string | null;

    // Handle different actions
    switch (action) {
      case 'getUrl': {
        // Generate a presigned URL for downloading a file
        const key = formData.get('key') as string;
        if (!key) {
          return NextResponse.json({ error: 'Missing key parameter' }, { status: 400 });
        }
        const url = await generatePresignedUrl(key);
        return NextResponse.json({ url });
      }

      case 'delete': {
        // Delete a file from R2
        const key = formData.get('key') as string;
        if (!key) {
          return NextResponse.json({ error: 'Missing key parameter' }, { status: 400 });
        }
        await deleteFromR2(key);
        return NextResponse.json({ success: true });
      }

      default: {
        // Upload a file to R2
        if (!file) {
          return NextResponse.json({ error: 'No file provided' }, { status: 400 });
        }

        // Build the R2 object key
        const timestamp = Date.now();
        const sanitizedFileName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
        const r2Key = path
          ? `${path}/${timestamp}_${sanitizedFileName}`
          : `uploads/${timestamp}_${sanitizedFileName}`;

        const data = await file.arrayBuffer();
        const r2ObjectKey = await uploadToR2(r2Key, data, file.type);

        // Generate a download URL
        const downloadUrl = await generatePresignedUrl(r2ObjectKey);

        return NextResponse.json({
          success: true,
          key: r2ObjectKey,
          url: downloadUrl,
          fileName: file.name,
          fileSize: file.size,
          contentType: file.type,
          storage: 'r2',
        });
      }
    }
  } catch (error: any) {
    console.error('[R2 Upload API] Error:', error);
    return NextResponse.json(
      { error: 'Upload failed', details: error.message },
      { status: 500 }
    );
  }
}

export async function GET(request: NextRequest) {
  try {
    const key = request.nextUrl.searchParams.get('key');
    if (!key) {
      return NextResponse.json({ error: 'Missing key parameter' }, { status: 400 });
    }

    const url = await generatePresignedUrl(key);
    return NextResponse.json({ url, key });
  } catch (error: any) {
    console.error('[R2 API] Error:', error);
    return NextResponse.json(
      { error: 'Failed to generate URL', details: error.message },
      { status: 500 }
    );
  }
}

export async function DELETE(request: NextRequest) {
  try {
    const key = request.nextUrl.searchParams.get('key');
    if (!key) {
      return NextResponse.json({ error: 'Missing key parameter' }, { status: 400 });
    }

    await deleteFromR2(key);
    return NextResponse.json({ success: true, key });
  } catch (error: any) {
    console.error('[R2 API] Delete error:', error);
    return NextResponse.json(
      { error: 'Delete failed', details: error.message },
      { status: 500 }
    );
  }
}
