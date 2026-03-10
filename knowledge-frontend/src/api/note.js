import request from './request.js'

/**
 * Get notes list with pagination, sorting, and filtering
 * @param {Object} params - { page, size, sort, order, tagId, categoryId }
 */
export function getNotes(params) {
  return request.get('/notes', { params })
}

/**
 * Get note detail by id
 * @param {number} id
 */
export function getNote(id) {
  return request.get(`/notes/${id}`)
}

/**
 * Create a new note
 * @param {Object} data - { title, content, categoryId, tagIds }
 */
export function createNote(data) {
  return request.post('/notes', data)
}

/**
 * Update an existing note
 * @param {number} id
 * @param {Object} data - { title, content, categoryId, tagIds }
 */
export function updateNote(id, data) {
  return request.put(`/notes/${id}`, data)
}

/**
 * Delete a note by id
 * @param {number} id
 */
export function deleteNote(id) {
  return request.delete(`/notes/${id}`)
}

/**
 * Full-text search notes
 * @param {Object} params - { q, page, size }
 */
export function searchNotes(params) {
  return request.get('/notes/search', { params })
}
