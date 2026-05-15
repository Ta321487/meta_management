import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { RESULT_OK, isAuthFailureCode } from '../constants/resultCodes'

const service = axios.create({
  baseURL: '/metadata-system/api',
  timeout: 30000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 添加认证信息到请求头
    const admin = sessionStorage.getItem('admin')
    if (admin) {
      // 对于基于Session的认证，浏览器会自动处理Cookie，这里确保请求携带凭证
      config.withCredentials = true
    }
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
    if (isAuthFailureCode(res.code)) {
      ElMessage.error('未登录，请先登录')
      // 清除过期的sessionStorage信息
      sessionStorage.removeItem('admin')
      // 确保重定向到登录页
      router.replace('/login')
      return Promise.reject(new Error('未登录'))
    }
    // 不在这里自动显示错误消息，让各个组件自己处理
    if (res.code !== RESULT_OK) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    // 处理网络错误等异常情况
    if (error.response && error.response.status === 401) {
      ElMessage.error('未登录，请先登录')
      // 清除过期的sessionStorage信息
      sessionStorage.removeItem('admin')
      // 确保重定向到登录页
      router.replace('/login')
    }
    return Promise.reject(error)
  }
)

export default service
