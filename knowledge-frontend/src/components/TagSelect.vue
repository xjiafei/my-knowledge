<template>
  <el-select
    v-model="selected"
    multiple
    filterable
    placeholder="选择标签（最多20个）"
    class="tag-select"
    @change="handleChange"
  >
    <el-option
      v-for="tag in tagStore.tags"
      :key="tag.id"
      :label="tag.name"
      :value="tag.id"
    />
  </el-select>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useTagStore } from '@/stores/tagStore.js'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const tagStore = useTagStore()

onMounted(() => {
  if (tagStore.tags.length === 0) {
    tagStore.fetchTags()
  }
})

const selected = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

function handleChange(val) {
  if (val.length > 20) {
    ElMessage.warning('最多关联 20 个标签')
    emit('update:modelValue', val.slice(0, 20))
  }
}
</script>

<style scoped>
.tag-select {
  width: 100%;
}
</style>
