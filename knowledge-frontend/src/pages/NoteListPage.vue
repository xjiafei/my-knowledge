<template>
  <div class="note-list-page">
    <!-- Filter indicator -->
    <div v-if="noteStore.hasFilter" class="filter-bar">
      <span class="filter-label">当前筛选：</span>
      <el-tag
        v-if="activeTag"
        type="primary"
        closable
        @close="noteStore.setTagFilter(null)"
      >
        标签：{{ activeTag.name }}
      </el-tag>
      <el-tag
        v-if="activeCategoryName"
        type="success"
        closable
        @close="noteStore.setCategoryFilter(null)"
        class="filter-tag"
      >
        分类：{{ activeCategoryName }}
      </el-tag>
      <el-button size="small" @click="noteStore.clearFilter()">
        清除筛选
      </el-button>
    </div>

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="sort-controls">
        <span class="sort-label">排序：</span>
        <el-select v-model="sortValue" size="small" style="width: 160px" @change="handleSortChange">
          <el-option label="更新时间（新→旧）" value="updatedAt_desc" />
          <el-option label="更新时间（旧→新）" value="updatedAt_asc" />
          <el-option label="创建时间（新→旧）" value="createdAt_desc" />
          <el-option label="创建时间（旧→新）" value="createdAt_asc" />
          <el-option label="标题（A→Z）" value="title_asc" />
          <el-option label="标题（Z→A）" value="title_desc" />
        </el-select>
      </div>
      <span class="total-count">共 {{ noteStore.total }} 条</span>
    </div>

    <!-- Loading state -->
    <div v-if="noteStore.loading" class="loading-state">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- Error state -->
    <el-alert
      v-else-if="noteStore.error"
      type="error"
      :title="noteStore.error"
      show-icon
      class="error-alert"
    />

    <!-- Empty state -->
    <EmptyState
      v-else-if="!noteStore.notes.length"
      :description="emptyDescription"
      action-text="新建笔记"
      @action="$router.push('/notes/new')"
    />

    <!-- Note list -->
    <div v-else class="note-list">
      <NoteCard
        v-for="note in noteStore.notes"
        :key="note.id"
        :note="note"
        @click="handleNoteClick"
        @edit="handleNoteEdit"
        @delete="handleNoteDelete"
      />
    </div>

    <!-- Pagination -->
    <Pagination
      v-if="noteStore.total > 0"
      :page="noteStore.page"
      :size="noteStore.size"
      :total="noteStore.total"
      @page-change="noteStore.setPage"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useNoteStore } from '@/stores/noteStore.js'
import { useTagStore } from '@/stores/tagStore.js'
import { useCategoryStore } from '@/stores/categoryStore.js'
import { deleteNote } from '@/api/note.js'
import NoteCard from '@/components/NoteCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const router = useRouter()
const noteStore = useNoteStore()
const tagStore = useTagStore()
const categoryStore = useCategoryStore()

const sortValue = ref('updatedAt_desc')

// Computed active filter labels
const activeTag = computed(() =>
  noteStore.tagId ? tagStore.tags.find(t => t.id === noteStore.tagId) : null
)

function findCategoryName(nodes, id) {
  for (const node of nodes) {
    if (node.id === id) return node.name
    if (node.children) {
      const found = findCategoryName(node.children, id)
      if (found) return found
    }
  }
  return null
}

const activeCategoryName = computed(() =>
  noteStore.categoryId ? findCategoryName(categoryStore.categories, noteStore.categoryId) : null
)

const emptyDescription = computed(() => {
  if (noteStore.tagId) return '该标签下暂无笔记'
  if (noteStore.categoryId) return '该分类下暂无笔记'
  return '暂无笔记，点击新建开始记录'
})

onMounted(() => {
  noteStore.fetchNotes()
  if (tagStore.tags.length === 0) tagStore.fetchTags()
  if (categoryStore.categories.length === 0) categoryStore.fetchCategories()
})

function handleSortChange(val) {
  const [field, direction] = val.split('_')
  noteStore.setSort(field, direction)
}

function handleSizeChange(newSize) {
  noteStore.size = newSize
  noteStore.page = 1
  noteStore.fetchNotes()
}

function handleNoteClick(note) {
  router.push(`/notes/${note.id}`)
}

function handleNoteEdit(note) {
  router.push(`/notes/${note.id}/edit`)
}

async function handleNoteDelete(note) {
  try {
    await deleteNote(note.id)
    ElMessage.success('删除成功')
    noteStore.fetchNotes()
  } catch {
    ElMessage.error('删除失败，请重试')
  }
}
</script>

<style scoped>
.note-list-page {
  max-width: 900px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #ecf5ff;
  border-radius: 6px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 14px;
  color: #606266;
}

.filter-tag {
  margin-left: 4px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.sort-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 14px;
  color: #606266;
}

.total-count {
  font-size: 14px;
  color: #909399;
}

.loading-state {
  padding: 20px 0;
}

.error-alert {
  margin-bottom: 16px;
}

.note-list {
  min-height: 200px;
}
</style>
