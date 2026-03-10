import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor - unified error handling
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      const msg = (data && data.message) || '网络异常，请检查连接'
      if (status === 404) {
        ElMessage.error(data?.message || '资源不存在')
      } else if (status === 409) {
        ElMessage.error(data?.message || '资源冲突')
      } else if (status === 400) {
        ElMessage.error(data?.message || '请求参数错误')
      } else {
        ElMessage.error(msg)
      }
    } else {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(error)
  }
)

export default request
