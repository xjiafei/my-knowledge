<template>
  <div class="file-list-page">
    <!-- Page Header -->
    <div class="page-header">
      <h1 class="page-title">文件管理</h1>
      <el-button
        type="primary"
        data-testid="upload-btn"
        @click="uploadDialogVisible = true"
      >
        上传文件
      </el-button>
    </div>

    <!-- Toolbar: Search + Category Filter -->
    <div class="toolbar">
      <el-input
        v-model="searchInput"
        placeholder="按文件名搜索..."
        clearable
        class="search-input"
        data-testid="file-search"
        @input="handleSearchInput"
        @clear="handleSearchClear"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <div class="category-filter" data-testid="category-filter">
        <el-radio-group
          v-model="selectedCategory"
          @change="handleCategoryChange"
        >
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PDF">PDF</el-radio-button>
          <el-radio-button value="DOCUMENT">文档</el-radio-button>
          <el-radio-button value="IMAGE">图片</el-radio-button>
          <el-radio-button value="TEXT">文本</el-radio-button>
          <el-radio-button value="ARCHIVE">压缩包</el-radio-button>
          <el-radio-button value="OTHER">其他</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- Tag Filter -->
    <div class="tag-filter">
      <el-select
        v-model="selectedTagId"
        clearable
        filterable
        placeholder="按标签筛选..."
        class="tag-select"
        @change="handleTagChange"
        @clear="handleTagClear"
      >
        <el-option
          v-for="tag in tagStore.tags"
          :key="tag.id"
          :label="tag.name"
          :value="tag.id"
        />
      </el-select>
      <el-button
        v-if="hasActiveFilters"
        link
        type="primary"
        @click="handleClearFilters"
      >
        清除筛选
      </el-button>
    </div>

    <!-- Loading State -->
    <div v-if="fileStore.loading" class="loading-wrapper">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- Error State -->
    <el-alert
      v-else-if="fileStore.error"
      :title="fileStore.error"
      type="error"
      :closable="false"
      class="error-alert"
    />

    <!-- Empty State -->
    <template v-else-if="fileStore.files.length === 0">
      <EmptyState
        v-if="!hasActiveFilters"
        description="暂无文件，点击上传添加知识文件"
        action-text="上传文件"
        @action="uploadDialogVisible = true"
      />
      <EmptyState
        v-else
        description="未找到相关文件"
        action-text="清除筛选"
        @action="handleClearFilters"
      />
    </template>

    <!-- File Table -->
    <el-table
      v-else
      :data="fileStore.files"
      row-key="id"
      :expand-row-keys="expandedRows"
      class="file-table"
      data-testid="file-table"
      @expand-change="handleExpandChange"
    >
      <!-- Expand Column (for description) -->
      <el-table-column type="expand" width="30">
        <template #default="{ row }">
          <div v-if="row.description" class="expand-description">
            <span class="description-label">描述：</span>
            <span>{{ row.description }}</span>
          </div>
          <div v-else class="expand-description no-description">
            暂无描述
          </div>
        </template>
      </el-table-column>

      <!-- Icon Column -->
      <el-table-column label="" width="44">
        <template #default="{ row }">
          <span class="file-icon" :class="getCategoryClass(row.fileCategory)">
            {{ getCategoryIcon(row.fileCategory) }}
          </span>
        </template>
      </el-table-column>

      <!-- File Name Column -->
      <el-table-column label="文件名" min-width="200">
        <template #default="{ row }">
          <el-tooltip :content="row.originalName" placement="top" :show-after="500">
            <span
              class="file-name-link"
              :class="{ clickable: row.description }"
              @click="row.description ? toggleExpand(row) : undefined"
            >
              {{ row.originalName }}
            </span>
          </el-tooltip>
        </template>
      </el-table-column>

      <!-- File Size Column -->
      <el-table-column label="大小" width="100">
        <template #default="{ row }">
          {{ row.fileSizeDisplay }}
        </template>
      </el-table-column>

      <!-- Upload Time Column -->
      <el-table-column label="上传时间" width="160">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>

      <!-- Tags Column -->
      <el-table-column label="标签" min-width="180">
        <template #default="{ row }">
          <template v-if="row.tags && row.tags.length > 0">
            <el-tag
              v-for="tag in row.tags.slice(0, 3)"
              :key="tag.id"
              size="small"
              class="file-tag"
              @click.stop="handleTagClick(tag.id)"
            >
              {{ tag.name }}
            </el-tag>
            <el-tag
              v-if="row.tags.length > 3"
              size="small"
              type="info"
            >
              +{{ row.tags.length - 3 }}
            </el-tag>
          </template>
          <span v-else class="no-tags">—</span>
        </template>
      </el-table-column>

      <!-- Actions Column -->
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-tooltip content="下载" placement="top">
            <el-button
              link
              type="primary"
              :data-testid="`download-btn-${row.id}`"
              @click="handleDownload(row)"
            >
              <el-icon><Download /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button
              link
              type="danger"
              :data-testid="`delete-btn-${row.id}`"
              @click="handleDelete(row)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <Pagination
      v-if="fileStore.total > 0"
      :page="fileStore.page"
      :size="fileStore.size"
      :total="fileStore.total"
      @page-change="fileStore.setPage"
      @size-change="fileStore.setSize"
    />

    <!-- Upload Dialog -->
    <FileUploadDialog
      v-model="uploadDialogVisible"
      @success="handleUploadSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download, Delete } from '@element-plus/icons-vue'
import { useFileStore } from '@/stores/fileStore.js'
import { useTagStore } from '@/stores/tagStore.js'
import { downloadFile, deleteFile } from '@/api/file.js'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'
import FileUploadDialog from '@/components/FileUploadDialog.vue'

const fileStore = useFileStore()
const tagStore = useTagStore()

const uploadDialogVisible = ref(false)
const searchInput = ref('')
const selectedCategory = ref('')
const selectedTagId = ref(null)
const expandedRows = ref([])

// Debounce timer for search
let searchTimer = null

const hasActiveFilters = computed(() => {
  return !!searchInput.value || !!selectedCategory.value || selectedTagId.value !== null
})

onMounted(() => {
  fileStore.fetchFiles()
  if (tagStore.tags.length === 0) {
    tagStore.fetchTags()
  }
})

// Category icon mapping
function getCategoryIcon(category) {
  const icons = {
    PDF: '📄',
    DOCUMENT: '📝',
    IMAGE: '🖼',
    TEXT: '📃',
    ARCHIVE: '📦',
    OTHER: '📎'
  }
  return icons[category] || '📎'
}

function getCategoryClass(category) {
  const classes = {
    PDF: 'icon-pdf',
    DOCUMENT: 'icon-document',
    IMAGE: 'icon-image',
    TEXT: 'icon-text',
    ARCHIVE: 'icon-archive',
    OTHER: 'icon-other'
  }
  return classes[category] || 'icon-other'
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// Search with debounce
function handleSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fileStore.setKeyword(searchInput.value)
  }, 300)
}

function handleSearchClear() {
  clearTimeout(searchTimer)
  searchInput.value = ''
  fileStore.setKeyword('')
}

function handleCategoryChange(val) {
  fileStore.setCategory(val)
}

function handleTagChange(val) {
  selectedTagId.value = val
  fileStore.setTagId(val)
}

function handleTagClear() {
  selectedTagId.value = null
  fileStore.setTagId(null)
}

function handleTagClick(tagId) {
  selectedTagId.value = tagId
  fileStore.setTagId(tagId)
}

function handleClearFilters() {
  searchInput.value = ''
  selectedCategory.value = ''
  selectedTagId.value = null
  fileStore.resetFilters()
}

// Expand row toggle
function toggleExpand(row) {
  const idx = expandedRows.value.indexOf(row.id)
  if (idx >= 0) {
    expandedRows.value.splice(idx, 1)
  } else {
    expandedRows.value.push(row.id)
  }
}

function handleExpandChange(row, expandedList) {
  expandedRows.value = expandedList.map(r => r.id)
}

// File Download (Blob approach)
async function handleDownload(file) {
  try {
    const response = await downloadFile(file.id)
    const contentType = response.headers?.['content-type'] || 'application/octet-stream'
    const blob = new Blob([response.data], { type: contentType })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = file.originalName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败，文件可能已被删除')
  }
}

// File Delete
async function handleDelete(file) {
  try {
    await ElMessageBox.confirm(
      `确认删除文件《${file.originalName}》？\n此操作将同时删除服务器上的文件，不可撤销。`,
      '确认删除',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    // User cancelled
    return
  }

  try {
    await deleteFile(file.id)
    ElMessage.success('删除成功')
    // If current page becomes empty and not on page 1, go back to page 1
    const remaining = fileStore.files.length - 1
    if (remaining === 0 && fileStore.page > 1) {
      fileStore.setPage(1)
    } else {
      fileStore.fetchFiles()
    }
  } catch (e) {
    ElMessage.error('删除失败，请重试')
  }
}

function handleUploadSuccess() {
  fileStore.setPage(1)
}
</script>

<style scoped>
.file-list-page {
  padding: 24px;
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.category-filter {
  flex: 1;
}

.tag-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.tag-select {
  width: 280px;
}

.loading-wrapper {
  padding: 24px 0;
}

.error-alert {
  margin-bottom: 16px;
}

.file-table {
  width: 100%;
  margin-bottom: 8px;
}

.file-icon {
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-name-link {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  max-width: 300px;
}

.file-name-link.clickable {
  cursor: pointer;
  color: #409eff;
}

.file-name-link.clickable:hover {
  text-decoration: underline;
}

.file-tag {
  margin-right: 4px;
  margin-bottom: 2px;
  cursor: pointer;
}

.file-tag:hover {
  opacity: 0.8;
}

.no-tags {
  color: #c0c4cc;
}

.expand-description {
  padding: 12px 24px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.expand-description .description-label {
  font-weight: 500;
  color: #303133;
}

.expand-description.no-description {
  color: #c0c4cc;
}
</style>
