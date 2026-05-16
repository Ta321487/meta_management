<#-- 生成前端路由配置（routes.js） -->
<#-- data: routes, businessCode -->
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
      <#if route.meta.title?has_content>title: ${route.meta.title?json_string},</#if>
      <#if route.meta.icon?has_content>icon: '${route.meta.icon}',</#if>
      businessCode: '${businessCode}'
    }
  }<#if route_has_next>,</#if>
</#list>
]
