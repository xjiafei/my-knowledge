import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getFiles } from '@/api/file.js'
import { ElMessage } from 'element-plus'

export const useFileStore = defineStore('file', () => {
  // State
  const files = ref([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(20)
  const loading = ref(false)
  const error = ref(null)

  // Filter state
  const fileCategory = ref('')
  const tagId = ref(null)
  const keyword = ref('')

  // Actions
  async function fetchFiles() {
    loading.value = true
    error.value = null
    try {
      const params = {
        page: page.value,
        size: size.value
      }
      if (fileCategory.value) params.fileCategory = fileCategory.value
      if (tagId.value !== null) params.tagId = tagId.value
      if (keyword.value) params.keyword = keyword.value

      const res = await getFiles(params)
      files.value = res.data.records || []
      total.value = res.data.total || 0
    } catch (err) {
      error.value = '加载失败，请重试'
      ElMessage.error('加载文件列表失败，请重试')
    } finally {
      loading.value = false
    }
  }

  function resetFilters() {
    fileCategory.value = ''
    tagId.value = null
    keyword.value = ''
    page.value = 1
    fetchFiles()
  }

  function setCategory(cat) {
    fileCategory.value = cat
    page.value = 1
    fetchFiles()
  }

  function setKeyword(kw) {
    keyword.value = kw
    page.value = 1
    fetchFiles()
  }

  function setTagId(id) {
    tagId.value = id
    page.value = 1
    fetchFiles()
  }

  function setPage(p) {
    page.value = p
    fetchFiles()
  }

  function setSize(s) {
    size.value = s
    page.value = 1
    fetchFiles()
  }

  return {
    files,
    total,
    page,
    size,
    loading,
    error,
    fileCategory,
    tagId,
    keyword,
    fetchFiles,
    resetFilters,
    setCategory,
    setKeyword,
    setTagId,
    setPage,
    setSize
  }
})
