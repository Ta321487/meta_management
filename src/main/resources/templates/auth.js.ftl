<#-- 生成前端认证API文件 -->
import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

export function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'post'
  })
}

<#if captchaEnabled>
export function getCaptcha() {
  return request({
    url: '/api/auth/captcha',
    method: 'get'
  })
}
</#if>
