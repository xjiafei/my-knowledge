import { ref } from 'vue'

/**
 * Reusable pagination state
 * @param {number} defaultSize - Default page size
 */
export function usePagination(defaultSize = 20) {
  const page = ref(1)
  const size = ref(defaultSize)
  const total = ref(0)

  function resetPage() {
    page.value = 1
  }

  function handlePageChange(newPage) {
    page.value = newPage
  }

  function handleSizeChange(newSize) {
    size.value = newSize
    page.value = 1
  }

  return {
    page,
    size,
    total,
    resetPage,
    handlePageChange,
    handleSizeChange
  }
}
