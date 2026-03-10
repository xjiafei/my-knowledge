<template>
  <div class="category-manage-page">
    <div class="page-header">
      <h2>分类管理</h2>
    </div>

    <div class="category-layout">
      <!-- Left: Category tree -->
      <div class="tree-panel">
        <div class="panel-title">分类结构</div>
        <div v-if="categoryStore.loading" class="loading-state">
          <el-skeleton :rows="5" animated />
        </div>
        <div v-else-if="!categoryStore.categories.length" class="tree-empty">
          暂无分类，请创建第一个分类
        </div>
        <el-tree
          v-else
          :data="categoryStore.categories"
          :props="treeProps"
          node-key="id"
          default-expand-all
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <span class="tree-node-label">
                {{ data.name }}
                <span class="node-count">({{ data.noteCount || 0 }})</span>
              </span>
              <div class="tree-node-actions">
                <el-button
                  size="small"
                  type="primary"
                  text
                  :icon="EditPen"
                  @click.stop="handleStartEdit(data)"
                />
                <el-popconfirm
                  :title="data.children && data.children.length > 0
                    ? '将同时删除所有子分类，相关笔记分类将置空，确定删除吗？'
                    : '相关笔记分类将置空，确定删除吗？'"
                  confirm-button-text="确定"
                  cancel-button-text="取消"
                  @confirm="handleDeleteCategory(data)"
                >
                  <template #reference>
                    <el-button
                      size="small"
                      type="danger"
                      text
                      :icon="Delete"
                      @click.stop
                    />
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </template>
        </el-tree>
      </div>

      <!-- Right: Create/Edit form -->
      <div class="form-panel">
        <div class="panel-title">{{ editingNode ? '编辑分类' : '新建分类' }}</div>

        <el-form :model="form" label-width="80px">
          <el-form-item label="分类名称">
            <el-input
              v-model="form.name"
              placeholder="请输入分类名称"
              @input="nameError = ''"
            />
            <div v-if="nameError" class="field-error">{{ nameError }}</div>
          </el-form-item>

          <el-form-item label="父分类" v-if="!editingNode">
            <el-select
              v-model="form.parentId"
              placeholder="选择父分类（可选，最多3级）"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="cat in eligibleParents"
                :key="cat.id"
                :label="'　'.repeat(cat.depth) + cat.name"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              {{ editingNode ? '保存修改' : '创建分类' }}
            </el-button>
            <el-button v-if="editingNode" @click="handleCancelEdit">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Delete } from '@element-plus/icons-vue'
import { createCategory, updateCategory, deleteCategory } from '@/api/category.js'
import { useCategoryStore } from '@/stores/categoryStore.js'

const categoryStore = useCategoryStore()

const treeProps = {
  children: 'children',
  label: 'name'
}

const form = reactive({
  name: '',
  parentId: null
})
const nameError = ref('')
const submitting = ref(false)
const editingNode = ref(null)

onMounted(() => {
  categoryStore.fetchCategories()
})

// Flatten categories for parent select, only depth 0 and 1 (max 2 levels as parent = 3 levels total)
const eligibleParents = computed(() => {
  const all = categoryStore.flattenCategories(categoryStore.categories)
  return all.filter(cat => cat.depth < 2) // depth 0 and 1 can be parents (resulting in depth 1 and 2 children)
})

function handleStartEdit(data) {
  editingNode.value = data
  form.name = data.name
}

function handleCancelEdit() {
  editingNode.value = null
  form.name = ''
}

async function handleSubmit() {
  const name = form.name.trim()
  if (!name) {
    nameError.value = '分类名称不能为空'
    return
  }

  submitting.value = true
  try {
    if (editingNode.value) {
      await updateCategory(editingNode.value.id, { name })
      ElMessage.success('更新成功')
      editingNode.value = null
    } else {
      await createCategory({
        name,
        parentId: form.parentId || null
      })
      ElMessage.success('创建成功')
    }
    form.name = ''
    form.parentId = null
    categoryStore.fetchCategories()
  } catch (err) {
    if (err.response?.status === 400) {
      ElMessage.error(err.response.data?.message || '操作失败')
    } else {
      ElMessage.error('操作失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

async function handleDeleteCategory(data) {
  try {
    await deleteCategory(data.id)
    ElMessage.success('删除成功')
    categoryStore.fetchCategories()
  } catch {
    ElMessage.error('删除失败，请重试')
  }
}
</script>

<style scoped>
.category-manage-page {
  max-width: 1000px;
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

.category-layout {
  display: flex;
  gap: 24px;
}

.tree-panel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  min-height: 400px;
}

.form-panel {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.loading-state {
  padding: 20px 0;
}

.tree-empty {
  color: #c0c4cc;
  font-size: 14px;
  text-align: center;
  padding: 40px 0;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}

.tree-node-label {
  flex: 1;
}

.node-count {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.tree-node-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}

.el-tree-node__content:hover .tree-node-actions {
  opacity: 1;
}

.field-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}
</style>
