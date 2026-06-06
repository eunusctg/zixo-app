/**
 * Cloudflare R2 Storage Service for Zixo
 *
 * Provides client-side functions to upload, download, and delete files
 * via the /api/upload API route which proxies to Cloudflare R2.
 *
 * Use this as an alternative to Firebase Storage for media files.
 * R2 offers lower egress costs and better global performance.
 */

// ==================== TYPES ====================

export interface R2UploadResult {
  key: string;
  url: string;
  fileName: string;
  fileSize: number;
  contentType: string;
  storage: 'r2';
}

export interface R2UploadProgress {
  uploadId: string;
  fileName: string;
  progress: number; // 0-100
  state: 'uploading' | 'success' | 'error';
  bytesTransferred: number;
  totalBytes: number;
  url?: string;
  error?: string;
}

export type R2UploadProgressCallback = (progress: R2UploadProgress) => void;

// ==================== UPLOAD ====================

/**
 * Upload a file to Cloudflare R2 via the API route
 *
 * @param file - The file to upload
 * @param path - Optional path prefix (e.g., 'avatars', 'chats/abc123')
 * @param onProgress - Optional progress callback
 * @returns Upload result with key, URL, and metadata
 */
export async function uploadToR2(
  file: File,
  path?: string,
  onProgress?: R2UploadProgressCallback,
): Promise<R2UploadResult> {
  const uploadId = `r2-${Date.now()}`;

  try {
    onProgress?.({
      uploadId,
      fileName: file.name,
      progress: 0,
      state: 'uploading',
      bytesTransferred: 0,
      totalBytes: file.size,
    });

    const formData = new FormData();
    formData.append('file', file);
    if (path) {
      formData.append('path', path);
    }

    // Use XMLHttpRequest for progress tracking
    const result = await new Promise<R2UploadResult>((resolve, reject) => {
      const xhr = new XMLHttpRequest();

      xhr.upload.addEventListener('progress', (event) => {
        if (event.lengthComputable) {
          const progress = Math.round((event.loaded / event.total) * 100);
          onProgress?.({
            uploadId,
            fileName: file.name,
            progress,
            state: 'uploading',
            bytesTransferred: event.loaded,
            totalBytes: event.total,
          });
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            const response = JSON.parse(xhr.responseText);
            resolve(response);
          } catch {
            reject(new Error('Invalid response from upload API'));
          }
        } else {
          reject(new Error(`Upload failed: ${xhr.status}`));
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('Network error during upload'));
      });

      xhr.addEventListener('abort', () => {
        reject(new Error('Upload aborted'));
      });

      xhr.open('POST', '/api/upload');
      xhr.send(formData);
    });

    onProgress?.({
      uploadId,
      fileName: file.name,
      progress: 100,
      state: 'success',
      bytesTransferred: file.size,
      totalBytes: file.size,
      url: result.url,
    });

    return result;
  } catch (error) {
    onProgress?.({
      uploadId,
      fileName: file.name,
      progress: 0,
      state: 'error',
      bytesTransferred: 0,
      totalBytes: file.size,
      error: error instanceof Error ? error.message : 'Upload failed',
    });
    throw error;
  }
}

// ==================== AVATAR UPLOAD ====================

/**
 * Upload a user avatar to R2
 *
 * @param uid - User ID
 * @param file - Avatar image file
 * @param onProgress - Optional progress callback
 * @returns R2 object URL
 */
export async function uploadAvatarToR2(
  uid: string,
  file: File,
  onProgress?: R2UploadProgressCallback,
): Promise<string> {
  // Compress image on client side before upload
  const compressedFile = await compressImage(file, {
    maxWidth: 512,
    maxHeight: 512,
    quality: 0.85,
    maxSizeKB: 512,
  });

  const result = await uploadToR2(compressedFile, `avatars/${uid}`, onProgress);
  return result.url;
}

// ==================== CHAT MEDIA UPLOAD ====================

/**
 * Upload chat media (images, voice notes, files) to R2
 *
 * @param chatId - Chat ID
 * @param senderId - Sender user ID
 * @param file - The file to upload
 * @param mediaType - Type of media
 * @param onProgress - Optional progress callback
 * @returns Upload result with download URL
 */
export async function uploadChatMediaToR2(
  chatId: string,
  senderId: string,
  file: File,
  mediaType: 'image' | 'voice' | 'file' = 'image',
  onProgress?: R2UploadProgressCallback,
): Promise<{ downloadUrl: string; fileName: string; fileSize: number; key: string }> {
  let fileToUpload = file;

  // Compress images before upload
  if (mediaType === 'image') {
    fileToUpload = await compressImage(file, {
      maxWidth: 1920,
      maxHeight: 1920,
      quality: 0.8,
      maxSizeKB: 2048,
    });
  }

  const result = await uploadToR2(
    fileToUpload,
    `chats/${chatId}/${mediaType}`,
    onProgress,
  );

  return {
    downloadUrl: result.url,
    fileName: file.name,
    fileSize: fileToUpload.size,
    key: result.key,
  };
}

// ==================== GET DOWNLOAD URL ====================

/**
 * Get a presigned download URL for an R2 object
 *
 * @param key - R2 object key
 * @returns Presigned URL (valid for 1 hour)
 */
export async function getR2DownloadUrl(key: string): Promise<string> {
  const response = await fetch(`/api/upload?key=${encodeURIComponent(key)}`);
  if (!response.ok) {
    throw new Error('Failed to get download URL');
  }
  const data = await response.json();
  return data.url;
}

// ==================== DELETE FILE ====================

/**
 * Delete a file from R2
 *
 * @param key - R2 object key
 */
export async function deleteFromR2(key: string): Promise<void> {
  const response = await fetch(`/api/upload?key=${encodeURIComponent(key)}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error('Failed to delete file from R2');
  }
}

// ==================== IMAGE COMPRESSION ====================

/**
 * Compress an image file before upload
 * Resizes large images and reduces quality to save bandwidth
 */
export function compressImage(
  file: File,
  options: {
    maxWidth?: number;
    maxHeight?: number;
    quality?: number;
    maxSizeKB?: number;
  } = {}
): Promise<File> {
  const {
    maxWidth = 1920,
    maxHeight = 1920,
    quality = 0.8,
    maxSizeKB = 1024,
  } = options;

  return new Promise((resolve, reject) => {
    if (!file.type.startsWith('image/')) {
      resolve(file);
      return;
    }

    if (file.size <= maxSizeKB * 1024) {
      resolve(file);
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let { width, height } = img;

        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height);
          width = Math.round(width * ratio);
          height = Math.round(height * ratio);
        }

        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          resolve(file);
          return;
        }

        ctx.drawImage(img, 0, 0, width, height);

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              resolve(file);
              return;
            }

            if (blob.size > maxSizeKB * 1024 && quality > 0.3) {
              compressImage(file, {
                ...options,
                quality: quality * 0.7,
                maxSizeKB,
              }).then(resolve).catch(reject);
              return;
            }

            const compressedFile = new File([blob], file.name, {
              type: file.type,
              lastModified: Date.now(),
            });
            resolve(compressedFile);
          },
          file.type,
          quality
        );
      };

      img.onerror = () => resolve(file);
      img.src = e.target?.result as string;
    };

    reader.onerror = () => resolve(file);
    reader.readAsDataURL(file);
  });
}
