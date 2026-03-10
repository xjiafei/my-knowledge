import request from './request.js'

/**
 * Get all tags (with noteCount)
 */
export function getTags() {
  return request.get('/tags')
}

/**
 * Create a new tag
 * @param {Object} data - { name }
 */
export function createTag(data) {
  return request.post('/tags', data)
}

/**
 * Update tag name
 * @param {number} id
 * @param {Object} data - { name }
 */
export function updateTag(id, data) {
  return request.put(`/tags/${id}`, data)
}

/**
 * Delete a tag by id
 * @param {number} id
 */
export function deleteTag(id) {
  return request.delete(`/tags/${id}`)
}
