<template>
  <div class="search-result-page">
    <!-- Search bar -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索笔记..."
        :prefix-icon="Search"
        clearable
        size="large"
        style="max-width: 600px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
    </div>

    <!-- Result count -->
    <div v-if="!loading && keyword" class="result-count">
      共找到 <strong>{{ total }}</strong> 条结果
      <span v-if="keyword">（关键词：{{ keyword }}）</span>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="6" animated />
    </div>

    <!-- Error state -->
    <el-alert
      v-else-if="error"
      type="error"
      :title="error"
      show-icon
      class="error-alert"
    />

    <!-- No keyword state -->
    <EmptyState
      v-else-if="!keyword"
      description="请输入关键词开始搜索"
    />

    <!-- Empty result state -->
    <EmptyState
      v-else-if="!results.length"
      description="未找到相关笔记"
    >
      <template v-if="!$slots.default">
        <p style="color: #909399; font-size: 14px;">试试其他关键词</p>
      </template>
    </EmptyState>

    <!-- Result list -->
    <div v-else class="result-list">
      <NoteCard
        v-for="note in results"
        :key="note.id"
        :note="note"
        :keyword="keyword"
        @click="handleNoteClick"
      />
    </div>

    <!-- Pagination -->
    <Pagination
      v-if="total > 0"
      :page="page"
      :size="size"
      :total="total"
      @page-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { searchNotes } from '@/api/note.js'
import NoteCard from '@/components/NoteCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const searchKeyword = ref('')
const results = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const error = ref(null)

onMounted(() => {
  const q = route.query.q
  if (q) {
    keyword.value = q
    searchKeyword.value = q
    doSearch()
  }
})

// Watch route query changes (from AppHeader search)
watch(() => route.query.q, (q) => {
  if (q && q !== keyword.value) {
    keyword.value = q
    searchKeyword.value = q
    page.value = 1
    doSearch()
  }
})

async function doSearch() {
  if (!keyword.value) return

  loading.value = true
  error.value = null
  try {
    const res = await searchNotes({
      q: keyword.value,
      page: page.value,
      size: size.value
    })
    results.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    error.value = '搜索失败，请重试'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  const q = searchKeyword.value.trim()
  if (!q) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  const truncated = q.length > 200 ? q.substring(0, 200) : q
  keyword.value = truncated
  page.value = 1
  router.push({ path: '/search', query: { q: truncated } })
  doSearch()
}

function handlePageChange(newPage) {
  page.value = newPage
  doSearch()
}

function handleNoteClick(note) {
  router.push(`/notes/${note.id}`)
}
</script>

<style scoped>
.search-result-page {
  max-width: 900px;
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
}

.result-count {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
}

.loading-state {
  padding: 20px 0;
}

.error-alert {
  margin-bottom: 16px;
}

.result-list {
  min-height: 200px;
}
</style>
