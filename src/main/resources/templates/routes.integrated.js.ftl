<#-- 生成业务系统下所有表的整合路由配置 -->
<#-- data: routes, businessCode, businessName -->
export default [
<#list routes as route>
  {
    path: '${route.path}',
    name: '${route.name}',
    component: ${route.component},
    meta: {
      <#if route.meta.moduleCode?has_content>moduleCode: '${route.meta.moduleCode}',</#if>
      <#if route.meta.nodeCode?has_content>nodeCode: '${route.meta.nodeCode}',</#if>
      relatedTableCode: '${route.meta.relatedTableCode}',
      isMenuVisible: ${route.meta.isMenuVisible},
      <#if route.meta.icon?has_content>icon: '${route.meta.icon}',</#if>
      businessCode: '${businessCode}'
    }
  }<#if route_has_next>,</#if>
</#list>
]