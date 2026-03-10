import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTags } from '@/api/tag.js'

export const useTagStore = defineStore('tag', () => {
  const tags = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchTags() {
    loading.value = true
    error.value = null
    try {
      const res = await getTags()
      tags.value = res.data || []
    } catch (err) {
      error.value = '加载标签失败'
    } finally {
      loading.value = false
    }
  }

  return {
    tags,
    loading,
    error,
    fetchTags
  }
})
