<template>
  <div class="note-detail-page">
    <!-- Loading state -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 404 state -->
    <div v-else-if="notFound" class="not-found">
      <el-result icon="warning" title="笔记不存在" sub-title="该笔记可能已被删除或ID无效">
        <template #extra>
          <el-button type="primary" @click="$router.push('/')">返回列表</el-button>
        </template>
      </el-result>
    </div>

    <!-- Note detail -->
    <template v-else-if="note">
      <!-- Action bar -->
      <div class="action-bar">
        <el-button :icon="ArrowLeft" @click="$router.push('/')">返回列表</el-button>
        <div class="action-right">
          <el-button type="primary" :icon="EditPen" @click="$router.push(`/notes/${note.id}/edit`)">
            编辑
          </el-button>
          <el-popconfirm
            title="确定删除此笔记吗？"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleDelete"
          >
            <template #reference>
              <el-button type="danger" :icon="Delete" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>

      <!-- Note content -->
      <div class="note-content-card">
        <h1 class="note-title">{{ note.title }}</h1>

        <!-- Meta info -->
        <div class="note-meta">
          <div class="meta-item" v-if="note.categoryName">
            <el-icon><FolderOpened /></el-icon>
            <span>{{ note.categoryName }}</span>
          </div>
          <div class="meta-item">
            <el-icon><Clock /></el-icon>
            <span>创建于 {{ formatTime(note.createdAt) }}</span>
          </div>
          <div class="meta-item">
            <el-icon><Refresh /></el-icon>
            <span>更新于 {{ formatTime(note.updatedAt) }}</span>
          </div>
        </div>

        <!-- Tags -->
        <div v-if="note.tags && note.tags.length > 0" class="note-tags">
          <el-tag
            v-for="tag in note.tags"
            :key="tag.id"
            class="note-tag"
            type="info"
            @click="handleTagClick(tag.id)"
            style="cursor: pointer;"
          >
            {{ tag.name }}
          </el-tag>
        </div>

        <el-divider />

        <!-- Markdown preview -->
        <MarkdownPreview :content="note.content" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, EditPen, Delete, FolderOpened, Clock, Refresh } from '@element-plus/icons-vue'
import { getNote, deleteNote } from '@/api/note.js'
import { useNoteStore } from '@/stores/noteStore.js'
import MarkdownPreview from '@/components/MarkdownPreview.vue'

const route = useRoute()
const router = useRouter()
const noteStore = useNoteStore()

const note = ref(null)
const loading = ref(false)
const notFound = ref(false)

onMounted(() => {
  fetchNote()
})

async function fetchNote() {
  loading.value = true
  notFound.value = false
  try {
    const res = await getNote(route.params.id)
    note.value = res.data
  } catch (err) {
    if (err.response?.status === 404) {
      notFound.value = true
    }
  } finally {
    loading.value = false
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function handleTagClick(tagId) {
  noteStore.setTagFilter(tagId)
  router.push('/')
}

async function handleDelete() {
  try {
    await deleteNote(note.value.id)
    ElMessage.success('删除成功')
    router.push('/')
  } catch {
    ElMessage.error('删除失败，请重试')
  }
}
</script>

<style scoped>
.note-detail-page {
  max-width: 900px;
  margin: 0 auto;
}

.loading-state {
  padding: 20px 0;
}

.not-found {
  padding: 60px 0;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.action-right {
  display: flex;
  gap: 8px;
}

.note-content-card {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.note-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 16px;
  color: #303133;
  line-height: 1.4;
}

.note-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.note-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}

.note-tag {
  margin: 0;
}
</style>
