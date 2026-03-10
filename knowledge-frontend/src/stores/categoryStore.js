import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCategories } from '@/api/category.js'

export const useCategoryStore = defineStore('category', () => {
  const categories = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchCategories() {
    loading.value = true
    error.value = null
    try {
      const res = await getCategories()
      categories.value = res.data || []
    } catch (err) {
      error.value = '加载分类失败'
    } finally {
      loading.value = false
    }
  }

  // Flatten tree for select options (max depth filtering)
  function flattenCategories(nodes, depth = 0) {
    const result = []
    for (const node of nodes) {
      result.push({ ...node, depth })
      if (node.children && node.children.length > 0) {
        result.push(...flattenCategories(node.children, depth + 1))
      }
    }
    return result
  }

  return {
    categories,
    loading,
    error,
    fetchCategories,
    flattenCategories
  }
})
