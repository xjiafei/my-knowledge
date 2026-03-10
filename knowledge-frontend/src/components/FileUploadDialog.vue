<template>
  <el-dialog
    v-model="dialogVisible"
    title="上传文件"
    width="560px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <div class="upload-dialog-body">
      <!-- File Dragger Area -->
      <el-upload
        ref="uploadRef"
        class="file-dragger"
        drag
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFileChange"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.jpg,.jpeg,.png,.gif,.webp,.txt,.md,.zip"
        data-testid="file-dragger"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          点击选择文件或拖拽至此处
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持：PDF、Word、Excel、PPT、图片、TXT、MD、ZIP（≤ 100MB）
          </div>
        </template>
      </el-upload>

      <!-- Selected File Info -->
      <div v-if="selectedFile" class="selected-file-info">
        <el-icon><Document /></el-icon>
        <span class="file-name">{{ selectedFile.name }}</span>
        <span class="file-size">（{{ formatFileSize(selectedFile.size) }}）</span>
        <el-button
          type="danger"
          link
          size="small"
          @click="clearSelectedFile"
        >
          移除
        </el-button>
      </div>

      <!-- Upload Progress -->
      <div v-if="selectedFile" class="upload-progress">
        <el-progress
          :percentage="uploadProgress"
          :status="uploadStatus === 'error' ? 'exception' : uploadStatus === 'success' ? 'success' : undefined"
          :striped="uploadStatus === 'uploading'"
          :striped-flow="uploadStatus === 'uploading'"
        />
      </div>

      <!-- Error Message -->
      <el-alert
        v-if="errorMsg"
        :title="errorMsg"
        type="error"
        :closable="false"
        class="upload-error"
      />

      <!-- Description -->
      <div class="form-item">
        <div class="form-label">文件描述（可选）</div>
        <el-input
          v-model="description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入文件描述..."
          data-testid="file-description"
        />
      </div>

      <!-- Tags -->
      <div class="form-item">
        <div class="form-label">标签（可选）</div>
        <TagSelect
          v-model="selectedTagIds"
          data-testid="file-tags"
        />
      </div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="uploadStatus === 'uploading'"
        :disabled="!selectedFile || uploadStatus === 'uploading'"
        data-testid="upload-confirm-btn"
        @click="submitUpload"
      >
        上传文件
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Document } from '@element-plus/icons-vue'
import { uploadFile } from '@/api/file.js'
import TagSelect from './TagSelect.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const dialogVisible = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

// Allowed extensions (lowercase)
const ALLOWED_EXTENSIONS = new Set([
  'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
  'jpg', 'jpeg', 'png', 'gif', 'webp', 'txt', 'md', 'zip'
])

const MAX_SIZE_BYTES = 100 * 1024 * 1024 // 100MB

const uploadRef = ref(null)
const selectedFile = ref(null)
const description = ref('')
const selectedTagIds = ref([])
const uploadProgress = ref(0)
const uploadStatus = ref('idle') // idle | selected | uploading | success | error
const errorMsg = ref('')

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function getExtension(filename) {
  const parts = filename.split('.')
  if (parts.length < 2) return ''
  return parts[parts.length - 1].toLowerCase()
}

function handleFileChange(file) {
  const rawFile = file.raw
  if (!rawFile) return

  // Validate extension
  const ext = getExtension(rawFile.name)
  if (!ALLOWED_EXTENSIONS.has(ext)) {
    ElMessage.error(`不支持该文件格式（.${ext}），请选择 PDF、Word、Excel、PPT、图片、TXT、MD 或 ZIP 文件`)
    clearSelectedFile()
    return
  }

  // Validate size
  if (rawFile.size > MAX_SIZE_BYTES) {
    ElMessage.error(`文件大小不能超过 100MB，当前文件大小为 ${formatFileSize(rawFile.size)}`)
    clearSelectedFile()
    return
  }

  selectedFile.value = rawFile
  uploadProgress.value = 0
  uploadStatus.value = 'selected'
  errorMsg.value = ''
}

function clearSelectedFile() {
  selectedFile.value = null
  uploadProgress.value = 0
  uploadStatus.value = 'idle'
  errorMsg.value = ''
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

async function submitUpload() {
  if (!selectedFile.value) return
  if (description.value.length > 500) {
    ElMessage.error('文件描述不能超过 500 字符')
    return
  }

  uploadStatus.value = 'uploading'
  uploadProgress.value = 0
  errorMsg.value = ''

  const formData = new FormData()
  formData.append('file', selectedFile.value)
  if (description.value) {
    formData.append('description', description.value)
  }
  selectedTagIds.value.forEach(id => formData.append('tagIds', id))

  try {
    await uploadFile(formData, (p) => {
      uploadProgress.value = p
    })
    uploadStatus.value = 'success'
    ElMessage.success('上传成功')
    emit('success')
    dialogVisible.value = false
  } catch (e) {
    uploadStatus.value = 'error'
    const serverMsg = e.response?.data?.message
    if (e.response?.status === 507) {
      ElMessageBox.alert('存储空间不足，请清理磁盘后重试', '存储空间不足', {
        type: 'error',
        confirmButtonText: '确定'
      })
      errorMsg.value = '存储空间不足，请清理磁盘后重试'
    } else {
      errorMsg.value = serverMsg || '上传失败，请重试'
    }
  }
}

function handleClosed() {
  // Reset state when dialog is closed
  selectedFile.value = null
  description.value = ''
  selectedTagIds.value = []
  uploadProgress.value = 0
  uploadStatus.value = 'idle'
  errorMsg.value = ''
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}
</script>

<style scoped>
.upload-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.file-dragger {
  width: 100%;
}

.selected-file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f0f2f5;
  border-radius: 6px;
  font-size: 14px;
  color: #303133;
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  color: #909399;
  flex-shrink: 0;
}

.upload-progress {
  padding: 0 4px;
}

.upload-error {
  margin: 0;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}
</style>
