import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const service = axios.create({
  baseURL: '/metadata-system/api',
  timeout: 30000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) {
      ElMessage.error('未登录，请先登录')
      router.push('/login')
      return Promise.reject(new Error('未登录'))
    }
    // 不在这里自动显示错误消息，让各个组件自己处理
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    // 网络错误等异常情况，也不自动显示，让组件处理
    return Promise.reject(error)
  }
)

export default service

