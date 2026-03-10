<template>
  <div class="tag-manage-page">
    <div class="page-header">
      <h2>标签管理</h2>
    </div>

    <!-- Create tag form -->
    <div class="create-form">
      <el-input
        v-model="newTagName"
        placeholder="输入标签名称"
        clearable
        style="width: 280px"
        @keyup.enter="handleCreateTag"
        @input="createError = ''"
      />
      <el-button type="primary" :loading="creating" @click="handleCreateTag">
        添加标签
      </el-button>
      <span v-if="createError" class="create-error">{{ createError }}</span>
    </div>

    <!-- Loading state -->
    <div v-if="tagStore.loading" class="loading-state">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- Empty state -->
    <EmptyState
      v-else-if="!tagStore.tags.length"
      description="暂无标签，请创建第一个标签"
    />

    <!-- Tag table -->
    <el-table v-else :data="tagStore.tags" style="width: 100%" class="tag-table">
      <el-table-column prop="name" label="标签名称" min-width="200">
        <template #default="{ row }">
          <div v-if="editingId === row.id" class="inline-edit">
            <el-input
              v-model="editingName"
              size="small"
              style="width: 200px"
              @keyup.enter="handleConfirmEdit(row)"
              @keyup.esc="handleCancelEdit"
              ref="editInputRef"
            />
            <el-button size="small" type="primary" @click="handleConfirmEdit(row)">确认</el-button>
            <el-button size="small" @click="handleCancelEdit">取消</el-button>
          </div>
          <span v-else>{{ row.name }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="noteCount" label="关联笔记数" width="120" align="center" />

      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button
            size="small"
            :icon="EditPen"
            @click="handleStartEdit(row)"
            v-if="editingId !== row.id"
          >
            编辑
          </el-button>
          <el-popconfirm
            :title="`删除后相关笔记将解除该标签，确定删除「${row.name}」吗？`"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleDeleteTag(row)"
          >
            <template #reference>
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                plain
                v-if="editingId !== row.id"
              >
                删除
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Delete } from '@element-plus/icons-vue'
import { createTag, updateTag, deleteTag } from '@/api/tag.js'
import { useTagStore } from '@/stores/tagStore.js'
import EmptyState from '@/components/EmptyState.vue'

const tagStore = useTagStore()

const newTagName = ref('')
const createError = ref('')
const creating = ref(false)

const editingId = ref(null)
const editingName = ref('')
const editInputRef = ref(null)

onMounted(() => {
  tagStore.fetchTags()
})

async function handleCreateTag() {
  const name = newTagName.value.trim()
  if (!name) {
    createError.value = '标签名称不能为空'
    return
  }

  creating.value = true
  try {
    await createTag({ name })
    ElMessage.success('创建成功')
    newTagName.value = ''
    tagStore.fetchTags()
  } catch (err) {
    if (err.response?.status === 409) {
      createError.value = '该标签已存在'
    } else {
      createError.value = '创建失败，请重试'
    }
  } finally {
    creating.value = false
  }
}

function handleStartEdit(row) {
  editingId.value = row.id
  editingName.value = row.name
  nextTick(() => {
    editInputRef.value?.focus()
  })
}

async function handleConfirmEdit(row) {
  const name = editingName.value.trim()
  if (!name) {
    ElMessage.warning('标签名称不能为空')
    return
  }
  if (name === row.name) {
    editingId.value = null
    return
  }

  try {
    await updateTag(row.id, { name })
    ElMessage.success('更新成功')
    editingId.value = null
    tagStore.fetchTags()
  } catch (err) {
    if (err.response?.status === 409) {
      ElMessage.error('该标签已存在')
    } else {
      ElMessage.error('更新失败，请重试')
    }
  }
}

function handleCancelEdit() {
  editingId.value = null
  editingName.value = ''
}

async function handleDeleteTag(row) {
  try {
    await deleteTag(row.id)
    ElMessage.success('删除成功')
    tagStore.fetchTags()
  } catch {
    ElMessage.error('删除失败，请重试')
  }
}
</script>

<style scoped>
.tag-manage-page {
  max-width: 900px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.create-form {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.create-error {
  color: #f56c6c;
  font-size: 13px;
}

.loading-state {
  padding: 20px 0;
}

.tag-table {
  border-radius: 8px;
  overflow: hidden;
}

.inline-edit {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
