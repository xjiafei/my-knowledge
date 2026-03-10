<template>
  <div class="category-tree">
    <el-tree
      :data="treeData"
      :props="treeProps"
      node-key="id"
      :highlight-current="true"
      :current-node-key="activeCategoryId"
      @node-click="handleNodeClick"
      empty-text="暂无分类"
    >
      <template #default="{ node, data }">
        <span class="tree-node">
          <span class="tree-node-label">{{ data.name }}</span>
          <span v-if="data.noteCount !== undefined" class="tree-node-count">
            ({{ data.noteCount }})
          </span>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  activeCategoryId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['filter'])

const treeProps = {
  children: 'children',
  label: 'name'
}

const treeData = computed(() => props.categories)

function handleNodeClick(data) {
  emit('filter', data.id)
}
</script>

<style scoped>
.category-tree {
  width: 100%;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tree-node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-count {
  font-size: 11px;
  color: #909399;
  flex-shrink: 0;
}
</style>
