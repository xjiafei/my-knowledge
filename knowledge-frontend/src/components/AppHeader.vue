<template>
  <header class="app-header">
    <div class="header-left">
      <router-link to="/" class="logo">
        <el-icon size="24"><Reading /></el-icon>
        <span class="logo-text">个人知识库</span>
      </router-link>
    </div>

    <div class="header-center">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索笔记..."
        :prefix-icon="Search"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="searchKeyword = ''"
      />
    </div>

    <div class="header-right">
      <el-button type="primary" :icon="Plus" @click="$router.push('/notes/new')">
        新建笔记
      </el-button>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, Plus, Reading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const searchKeyword = ref(route.query.q || '')

function handleSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  // Truncate to 200 chars if exceeded
  const q = keyword.length > 200 ? keyword.substring(0, 200) : keyword
  router.push({ path: '/search', query: { q } })
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  padding: 0 24px;
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-left {
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #303133;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
  white-space: nowrap;
}

.header-center {
  flex: 1;
  max-width: 500px;
  margin: 0 24px;
}

.search-input {
  width: 100%;
}

.header-right {
  flex-shrink: 0;
}
</style>
