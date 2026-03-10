<template>
  <div class="tag-cloud">
    <span
      v-for="tag in tags"
      :key="tag.id"
      class="tag-cloud-item"
      :class="{ active: activeTagId === tag.id }"
      :style="{ fontSize: getFontSize(tag.noteCount) + 'px' }"
      @click="handleTagClick(tag.id)"
    >
      {{ tag.name }}
      <span class="tag-count">({{ tag.noteCount || 0 }})</span>
    </span>
    <div v-if="!tags || tags.length === 0" class="tag-cloud-empty">
      暂无标签
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tags: {
    type: Array,
    default: () => []
  },
  activeTagId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['filter'])

const minCount = computed(() => {
  if (!props.tags || props.tags.length === 0) return 0
  return Math.min(...props.tags.map(t => t.noteCount || 0))
})

const maxCount = computed(() => {
  if (!props.tags || props.tags.length === 0) return 0
  return Math.max(...props.tags.map(t => t.noteCount || 0))
})

const MIN_FONT = 12
const MAX_FONT = 24

function getFontSize(count) {
  const min = minCount.value
  const max = maxCount.value
  if (max === min) return (MIN_FONT + MAX_FONT) / 2
  return MIN_FONT + ((count - min) / (max - min)) * (MAX_FONT - MIN_FONT)
}

function handleTagClick(tagId) {
  emit('filter', tagId)
}
</script>

<style scoped>
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px 0;
}

.tag-cloud-item {
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
  line-height: 1.6;
  padding: 2px 0;
}

.tag-cloud-item:hover {
  color: #409eff;
}

.tag-cloud-item.active {
  color: #409eff;
  font-weight: 600;
}

.tag-count {
  font-size: 11px;
  color: #909399;
}

.tag-cloud-empty {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
