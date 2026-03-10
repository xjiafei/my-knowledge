import request from './request.js'

/**
 * Get file list with pagination and filtering
 * @param {Object} params - { page, size, fileCategory, tagId, keyword }
 */
export function getFiles(params) {
  return request.get('/files', { params })
}

/**
 * Upload a file with progress callback
 * @param {FormData} formData - { file, description, tagIds[] }
 * @param {Function} onProgress - callback(percentage: 0-100)
 */
export function uploadFile(formData, onProgress) {
  return request.post('/files', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: e => onProgress?.(Math.round((e.loaded * 100) / e.total))
  })
}

/**
 * Download a file as blob
 * @param {number} id
 */
export function downloadFile(id) {
  return request.get(`/files/${id}/download`, { responseType: 'blob' })
}

/**
 * Delete a file by id
 * @param {number} id
 */
export function deleteFile(id) {
  return request.delete(`/files/${id}`)
}
