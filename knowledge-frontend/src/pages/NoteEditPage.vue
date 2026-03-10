<template>
  <div class="note-edit-page">
    <!-- 404 state for edit mode -->
    <div v-if="notFound" class="not-found">
      <el-result icon="warning" title="笔记不存在" sub-title="该笔记可能已被删除或ID无效">
        <template #extra>
          <el-button type="primary" @click="$router.push('/')">返回列表</el-button>
        </template>
      </el-result>
    </div>

    <template v-else>
      <!-- Loading state for edit mode -->
      <div v-if="initialLoading" class="loading-state">
        <el-skeleton :rows="8" animated />
      </div>

      <template v-else>
        <!-- Top form row -->
        <div class="edit-form-row">
          <div class="title-input-wrapper">
            <el-input
              v-model="form.title"
              placeholder="请输入标题（必填）"
              size="large"
              class="title-input"
              :class="{ 'has-error': titleError }"
              @input="titleError = ''"
            />
            <div v-if="titleError" class="field-error">{{ titleError }}</div>
          </div>

          <el-select
            v-model="form.categoryId"
            placeholder="选择分类（可选）"
            clearable
            class="category-select"
          >
            <el-option
              v-for="cat in flatCategories"
              :key="cat.id"
              :label="'　'.repeat(cat.depth) + cat.name"
              :value="cat.id"
            />
          </el-select>

          <TagSelect v-model="form.tagIds" class="tag-select" />
        </div>

        <!-- Editor area -->
        <div class="editor-area">
          <div class="editor-pane">
            <div class="pane-header">
              <span>Markdown 编辑</span>
            </div>
            <MarkdownEditor
              v-model="form.content"
              placeholder="请输入 Markdown 内容..."
              class="editor-content"
            />
          </div>
          <div class="preview-pane">
            <div class="pane-header">
              <span>实时预览</span>
            </div>
            <div class="preview-content">
              <MarkdownPreview :content="debouncedContent" />
            </div>
          </div>
        </div>

        <!-- Bottom action bar -->
        <div class="action-bar">
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">
            保存
          </el-button>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNote, createNote, updateNote } from '@/api/note.js'
import { useCategoryStore } from '@/stores/categoryStore.js'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import TagSelect from '@/components/TagSelect.vue'

const route = useRoute()
const router = useRouter()
const categoryStore = useCategoryStore()

const isEditMode = computed(() => !!route.params.id)
const noteId = computed(() => route.params.id)

const initialLoading = ref(false)
const saving = ref(false)
const notFound = ref(false)
const titleError = ref('')

const form = reactive({
  title: '',
  content: '',
  categoryId: null,
  tagIds: []
})

// Debounced content for preview
const debouncedContent = ref('')
let debounceTimer = null

watch(() => form.content, (newVal) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    debouncedContent.value = newVal
  }, 300)
})

// Flatten categories for select (max 2 levels can be parent, so depth 0 and 1)
const flatCategories = computed(() => {
  return categoryStore.flattenCategories(categoryStore.categories)
})

onMounted(async () => {
  if (categoryStore.categories.length === 0) {
    categoryStore.fetchCategories()
  }

  if (isEditMode.value) {
    await loadNote()
  }
})

async function loadNote() {
  initialLoading.value = true
  try {
    const res = await getNote(noteId.value)
    const note = res.data
    form.title = note.title
    form.content = note.content || ''
    debouncedContent.value = note.content || ''
    form.categoryId = note.categoryId || null
    form.tagIds = (note.tags || []).map(t => t.id)
  } catch (err) {
    if (err.response?.status === 404) {
      notFound.value = true
    }
  } finally {
    initialLoading.value = false
  }
}

function validateForm() {
  if (!form.title.trim()) {
    titleError.value = '标题不能为空'
    return false
  }
  if (form.tagIds.length > 20) {
    ElMessage.warning('最多关联 20 个标签')
    return false
  }
  return true
}

async function handleSave() {
  if (!validateForm()) return

  saving.value = true
  try {
    const data = {
      title: form.title.trim(),
      content: form.content,
      categoryId: form.categoryId || null,
      tagIds: form.tagIds
    }

    let res
    if (isEditMode.value) {
      res = await updateNote(noteId.value, data)
    } else {
      res = await createNote(data)
    }

    ElMessage.success('保存成功')
    const savedNote = res.data
    router.push(`/notes/${savedNote.id}`)
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  if (isEditMode.value) {
    router.push(`/notes/${noteId.value}`)
  } else {
    router.push('/')
  }
}
</script>

<style scoped>
.note-edit-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 108px);
}

.not-found {
  padding: 60px 0;
}

.loading-state {
  padding: 20px 0;
}

.edit-form-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: flex-start;
}

.title-input-wrapper {
  flex: 1;
}

.title-input {
  width: 100%;
}

.title-input.has-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #f56c6c inset;
}

.field-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}

.category-select {
  width: 200px;
}

.tag-select {
  width: 240px;
}

.editor-area {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.editor-pane,
.preview-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.pane-header {
  padding: 8px 16px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  font-size: 13px;
  color: #606266;
  font-weight: 600;
}

.editor-content {
  flex: 1;
  border-radius: 0 0 4px 4px;
}

.preview-content {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 0 0 4px 4px;
  padding: 16px;
  overflow-y: auto;
  background: #fff;
  min-height: 400px;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
  margin-top: 16px;
}
</style>
