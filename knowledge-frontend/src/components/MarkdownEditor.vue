<template>
  <textarea
    class="markdown-editor"
    :value="modelValue"
    :placeholder="placeholder"
    @input="$emit('update:modelValue', $event.target.value)"
  ></textarea>
</template>

<script setup>
defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入 Markdown 内容...'
  }
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.markdown-editor {
  width: 100%;
  height: 100%;
  min-height: 400px;
  padding: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  resize: none;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.markdown-editor:focus {
  border-color: #409eff;
}
</style>
