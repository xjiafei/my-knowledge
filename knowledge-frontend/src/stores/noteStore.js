import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getNotes } from '@/api/note.js'
import { ElMessage } from 'element-plus'

export const useNoteStore = defineStore('note', () => {
  // State
  const notes = ref([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(20)
  const loading = ref(false)
  const error = ref(null)

  // Filter state
  const tagId = ref(null)
  const categoryId = ref(null)

  // Sort state
  const sort = ref('updatedAt')
  const order = ref('desc')

  // Computed
  const hasFilter = computed(() => tagId.value !== null || categoryId.value !== null)

  // Actions
  async function fetchNotes() {
    loading.value = true
    error.value = null
    try {
      const params = {
        page: page.value,
        size: size.value,
        sort: sort.value,
        order: order.value
      }
      if (tagId.value !== null) params.tagId = tagId.value
      if (categoryId.value !== null) params.categoryId = categoryId.value

      const res = await getNotes(params)
      notes.value = res.data.records || []
      total.value = res.data.total || 0
    } catch (err) {
      error.value = '加载失败，请重试'
    } finally {
      loading.value = false
    }
  }

  function setTagFilter(id) {
    tagId.value = id
    page.value = 1
    fetchNotes()
  }

  function setCategoryFilter(id) {
    categoryId.value = id
    page.value = 1
    fetchNotes()
  }

  function clearFilter() {
    tagId.value = null
    categoryId.value = null
    page.value = 1
    fetchNotes()
  }

  function setSort(field, direction) {
    sort.value = field
    order.value = direction
    page.value = 1
    fetchNotes()
  }

  function setPage(p) {
    page.value = p
    fetchNotes()
  }

  return {
    notes,
    total,
    page,
    size,
    loading,
    error,
    tagId,
    categoryId,
    sort,
    order,
    hasFilter,
    fetchNotes,
    setTagFilter,
    setCategoryFilter,
    clearFilter,
    setSort,
    setPage
  }
})
