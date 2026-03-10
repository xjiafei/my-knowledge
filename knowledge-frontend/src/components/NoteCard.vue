<template>
  <el-card class="note-card" shadow="hover" @click="handleCardClick">
    <div class="note-card-header">
      <h3 class="note-title" v-html="titleHtml"></h3>
      <div class="note-actions" @click.stop>
        <el-button
          size="small"
          :icon="EditPen"
          circle
          @click="$emit('edit', note)"
          title="编辑"
        />
        <el-popconfirm
          title="确定删除此笔记吗？"
          confirm-button-text="确定"
          cancel-button-text="取消"
          @confirm="$emit('delete', note)"
        >
          <template #reference>
            <el-button
              size="small"
              :icon="Delete"
              circle
              type="danger"
              plain
              title="删除"
            />
          </template>
        </el-popconfirm>
      </div>
    </div>
    <p class="note-summary" v-html="summaryHtml"></p>
    <div class="note-footer">
      <div class="note-tags">
        <el-tag
          v-for="tag in note.tags"
          :key="tag.id"
          size="small"
          type="info"
          class="note-tag"
        >
          {{ tag.name }}
        </el-tag>
      </div>
      <span class="note-time">{{ formatTime(note.updatedAt) }}</span>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { EditPen, Delete } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'

const props = defineProps({
  note: {
    type: Object,
    required: true
  },
  keyword: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['click', 'edit', 'delete'])

function highlightText(text, keyword) {
  if (!text) return ''
  // Escape the raw text to prevent XSS before adding <mark> tags
  const escaped_text = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  if (!keyword) return escaped_text
  const escapedKw = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const highlighted = escaped_text.replace(new RegExp(escapedKw, 'gi'), '<mark>$&</mark>')
  return DOMPurify.sanitize(highlighted, { ALLOWED_TAGS: ['mark'] })
}

const titleHtml = computed(() => highlightText(props.note.title, props.keyword))
const summaryHtml = computed(() => highlightText(props.note.summary || '', props.keyword))

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

function handleCardClick() {
  emit('click', props.note)
}
</script>

<style scoped>
.note-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.note-card:hover {
  transform: translateY(-2px);
}

.note-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.note-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  margin-right: 12px;
  line-height: 1.4;
}

.note-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.note-summary {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.note-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.note-tag {
  margin: 0;
}

.note-time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 8px;
}
</style>
