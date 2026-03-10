<template>
  <aside class="app-sidebar" :class="{ collapsed }">
    <div class="sidebar-toggle" @click="collapsed = !collapsed">
      <el-icon>
        <component :is="collapsed ? 'Expand' : 'Fold'" />
      </el-icon>
    </div>

    <div v-show="!collapsed" class="sidebar-content">
      <!-- Category Tree Section -->
      <div class="sidebar-section">
        <div class="sidebar-section-header">
          <el-icon><FolderOpened /></el-icon>
          <span>分类</span>
        </div>
        <div v-if="categoryStore.loading" class="sidebar-loading">
          <el-skeleton :rows="3" animated />
        </div>
        <CategoryTree
          v-else
          :categories="categoryStore.categories"
          :active-category-id="noteStore.categoryId"
          @filter="handleCategoryFilter"
        />
      </div>

      <!-- Tag Cloud Section -->
      <div class="sidebar-section">
        <div class="sidebar-section-header">
          <el-icon><Collection /></el-icon>
          <span>标签</span>
        </div>
        <div v-if="tagStore.loading" class="sidebar-loading">
          <el-skeleton :rows="2" animated />
        </div>
        <TagCloud
          v-else
          :tags="tagStore.tags"
          :active-tag-id="noteStore.tagId"
          @filter="handleTagFilter"
        />
      </div>

      <!-- Quick Links -->
      <div class="sidebar-section">
        <div class="sidebar-section-header">
          <el-icon><Setting /></el-icon>
          <span>管理</span>
        </div>
        <div class="sidebar-links">
          <router-link to="/tags" class="sidebar-link">
            <el-icon><PriceTag /></el-icon>
            标签管理
          </router-link>
          <router-link to="/categories" class="sidebar-link">
            <el-icon><FolderOpened /></el-icon>
            分类管理
          </router-link>
          <router-link to="/files" class="sidebar-link">
            <el-icon><Files /></el-icon>
            文件管理
          </router-link>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { FolderOpened, Collection, Setting, PriceTag, Files } from '@element-plus/icons-vue'
import { useNoteStore } from '@/stores/noteStore.js'
import { useTagStore } from '@/stores/tagStore.js'
import { useCategoryStore } from '@/stores/categoryStore.js'
import CategoryTree from './CategoryTree.vue'
import TagCloud from './TagCloud.vue'

const noteStore = useNoteStore()
const tagStore = useTagStore()
const categoryStore = useCategoryStore()

const collapsed = ref(false)

onMounted(() => {
  tagStore.fetchTags()
  categoryStore.fetchCategories()
})

function handleTagFilter(tagId) {
  if (noteStore.tagId === tagId) {
    noteStore.setTagFilter(null)
  } else {
    noteStore.setTagFilter(tagId)
  }
}

function handleCategoryFilter(categoryId) {
  if (noteStore.categoryId === categoryId) {
    noteStore.setCategoryFilter(null)
  } else {
    noteStore.setCategoryFilter(categoryId)
  }
}
</script>

<style scoped>
.app-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  min-height: calc(100vh - 60px);
  position: relative;
  transition: width 0.3s;
}

.app-sidebar.collapsed {
  width: 40px;
}

.sidebar-toggle {
  position: absolute;
  top: 12px;
  right: -14px;
  width: 28px;
  height: 28px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.sidebar-toggle:hover {
  background: #f0f2f5;
}

.sidebar-content {
  padding: 16px;
  overflow: hidden;
}

.sidebar-section {
  margin-bottom: 24px;
}

.sidebar-section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.sidebar-loading {
  padding: 8px 0;
}

.sidebar-links {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 6px;
  color: #606266;
  text-decoration: none;
  font-size: 14px;
  transition: background 0.2s;
}

.sidebar-link:hover {
  background: #f0f2f5;
  color: #409eff;
}

.sidebar-link.router-link-active {
  background: #ecf5ff;
  color: #409eff;
}
</style>
