/**
 * Firebase Cloud Storage Service for Zixo
 * Handles file uploads for avatars, chat media, voice notes, and documents.
 * Includes image compression and upload progress tracking.
 */

import {
  ref,
  uploadBytesResumable,
  getDownloadURL,
  deleteObject,
  uploadBytes,
  type UploadTaskSnapshot,
} from 'firebase/storage';
import { storage } from './firebase';

// ==================== TYPES ====================

export interface UploadProgress {
  uploadId: string;
  fileName: string;
  progress: number; // 0-100
  state: 'running' | 'paused' | 'success' | 'error';
  bytesTransferred: number;
  totalBytes: number;
  downloadUrl?: string;
  error?: string;
}

export type UploadProgressCallback = (progress: UploadProgress) => void;

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
    quality?: number; // 0-1
    maxSizeKB?: number;
  } = {}
): Promise<File> {
  const {
    maxWidth = 1920,
    maxHeight = 1920,
    quality = 0.8,
    maxSizeKB = 1024, // Target max 1MB
  } = options;

  return new Promise((resolve, reject) => {
    // Only compress image files
    if (!file.type.startsWith('image/')) {
      resolve(file);
      return;
    }

    // Skip compression for small files
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

        // Resize while maintaining aspect ratio
        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height);
          width = Math.round(width * ratio);
          height = Math.round(height * ratio);
        }

        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          resolve(file); // Fall back to original
          return;
        }

        ctx.drawImage(img, 0, 0, width, height);

        // Try with the specified quality first
        canvas.toBlob(
          (blob) => {
            if (!blob) {
              resolve(file); // Fall back to original
              return;
            }

            // If still too large, reduce quality further
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

      img.onerror = () => resolve(file); // Fall back to original
      img.src = e.target?.result as string;
    };

    reader.onerror = () => resolve(file); // Fall back to original
    reader.readAsDataURL(file);
  });
}

// ==================== AVATAR UPLOAD ====================

/**
 * Upload a user avatar image to Firebase Storage
 * Compresses the image before uploading
 */
export async function uploadAvatar(
  uid: string,
  file: File,
  onProgress?: UploadProgressCallback
): Promise<string> {
  const uploadId = `avatar-${Date.now()}`;

  try {
    // Compress the image
    const compressedFile = await compressImage(file, {
      maxWidth: 512,
      maxHeight: 512,
      quality: 0.85,
      maxSizeKB: 512,
    });

    const storageRef = ref(storage, `avatars/${uid}/profile.${file.name.split('.').pop() || 'jpg'}`);

    return new Promise((resolve, reject) => {
      const uploadTask = uploadBytesResumable(storageRef, compressedFile);

      uploadTask.on(
        'state_changed',
        (snapshot: UploadTaskSnapshot) => {
          const progress = (snapshot.bytesTransferred / snapshot.totalBytes) * 100;
          onProgress?.({
            uploadId,
            fileName: file.name,
            progress: Math.round(progress),
            state: snapshot.state === 'running' ? 'running' : snapshot.state === 'paused' ? 'paused' : 'running',
            bytesTransferred: snapshot.bytesTransferred,
            totalBytes: snapshot.totalBytes,
          });
        },
        (error) => {
          onProgress?.({
            uploadId,
            fileName: file.name,
            progress: 0,
            state: 'error',
            bytesTransferred: 0,
            totalBytes: file.size,
            error: error.message,
          });
          reject(error);
        },
        async () => {
          const downloadUrl = await getDownloadURL(uploadTask.snapshot.ref);
          onProgress?.({
            uploadId,
            fileName: file.name,
            progress: 100,
            state: 'success',
            bytesTransferred: file.size,
            totalBytes: file.size,
            downloadUrl,
          });
          resolve(downloadUrl);
        }
      );
    });
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

// ==================== CHAT MEDIA UPLOAD ====================

/**
 * Upload chat media (images, voice notes, files)
 */
export async function uploadChatMedia(
  chatId: string,
  senderId: string,
  file: File,
  mediaType: 'image' | 'voice' | 'file' = 'image',
  onProgress?: UploadProgressCallback
): Promise<{ downloadUrl: string; fileName: string; fileSize: number }> {
  const uploadId = `chat-${chatId}-${Date.now()}`;
  const timestamp = Date.now();
  const sanitizedFileName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
  const filePath = `chats/${chatId}/${mediaType}/${senderId}_${timestamp}_${sanitizedFileName}`;

  try {
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

    const storageRef = ref(storage, filePath);

    return new Promise((resolve, reject) => {
      const uploadTask = uploadBytesResumable(storageRef, fileToUpload);

      uploadTask.on(
        'state_changed',
        (snapshot: UploadTaskSnapshot) => {
          const progress = (snapshot.bytesTransferred / snapshot.totalBytes) * 100;
          onProgress?.({
            uploadId,
            fileName: file.name,
            progress: Math.round(progress),
            state: snapshot.state === 'running' ? 'running' : snapshot.state === 'paused' ? 'paused' : 'running',
            bytesTransferred: snapshot.bytesTransferred,
            totalBytes: snapshot.totalBytes,
          });
        },
        (error) => {
          onProgress?.({
            uploadId,
            fileName: file.name,
            progress: 0,
            state: 'error',
            bytesTransferred: 0,
            totalBytes: file.size,
            error: error.message,
          });
          reject(error);
        },
        async () => {
          const downloadUrl = await getDownloadURL(uploadTask.snapshot.ref);
          onProgress?.({
            uploadId,
            fileName: file.name,
            progress: 100,
            state: 'success',
            bytesTransferred: file.size,
            totalBytes: file.size,
            downloadUrl,
          });
          resolve({
            downloadUrl,
            fileName: file.name,
            fileSize: fileToUpload.size,
          });
        }
      );
    });
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

// ==================== SIMPLE UPLOAD (no progress) ====================

/**
 * Simple upload without progress tracking (for small files)
 */
export async function uploadFileSimple(
  path: string,
  file: File | Blob
): Promise<string> {
  const storageRef = ref(storage, path);
  await uploadBytes(storageRef, file);
  return getDownloadURL(storageRef);
}

// ==================== DOWNLOAD URL ====================

/**
 * Get a download URL for a file in Firebase Storage
 */
export async function getFileDownloadUrl(filePath: string): Promise<string> {
  const storageRef = ref(storage, filePath);
  return getDownloadURL(storageRef);
}

// ==================== DELETE FILE ====================

/**
 * Delete a file from Firebase Storage
 */
export async function deleteFile(filePath: string): Promise<void> {
  const storageRef = ref(storage, filePath);
  await deleteObject(storageRef);
}

/**
 * Delete a file by its download URL (extracts the path)
 */
export async function deleteFileByUrl(downloadUrl: string): Promise<void> {
  try {
    const storageRef = ref(storage, downloadUrl);
    await deleteObject(storageRef);
  } catch {
    // File may already be deleted - ignore
  }
}
