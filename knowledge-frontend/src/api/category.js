import request from './request.js'

/**
 * Get categories tree
 */
export function getCategories() {
  return request.get('/categories')
}

/**
 * Create a new category
 * @param {Object} data - { name, parentId }
 */
export function createCategory(data) {
  return request.post('/categories', data)
}

/**
 * Update category name
 * @param {number} id
 * @param {Object} data - { name }
 */
export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

/**
 * Delete a category (cascades to children)
 * @param {number} id
 */
export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}
