<#-- 生成前端API统一导出文件 -->
<#-- 此文件用于将所有生成的API导入并重新导出，方便组件统一导入 -->

<#-- 导入auth.js -->
import { AuthApi } from './auth'

<#-- 导入其他业务模块API -->
<#list tableCodes as tableCode>
<#assign className = tableCode?capitalize>
import { ${className}Api } from './${tableCode?lower_case}Api'
</#list>

<#-- 统一导出所有API -->
export {
  AuthApi,
  <#list tableCodes as tableCode>
  <#assign className = tableCode?capitalize>
  ${className}Api<#if tableCode_has_next>,</#if>
  </#list>
}