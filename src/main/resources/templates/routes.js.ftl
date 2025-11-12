<#-- 生成前端路由配置（routes.js） -->
<#-- data: table, componentName, componentDir, nodes -->
export default [
<#if nodes?has_content>
  <#list nodes as node>
    <#-- 仅对已配置 jumpRelation 的节点生成路由 -->
    <#if node.jumpRelation?has_content>
    {
      path: '${node.jumpRelation}',
      name: '${node.nodeCode}',
      <#-- 根据 nodeType 选择组件名（LIST -> List.vue, 其它 -> Form.vue） -->
  <#-- 支持 nodeType 包含 LIST、FORM、DETAIL 等关键词 -->
  <#if (node.nodeType?upper_case?contains('LIST'))>
  component: () => import('@/views/${componentDir}/List.vue'),
  <#elseif (node.nodeType?upper_case?contains('FORM'))>
  component: () => import('@/views/${componentDir}/Form.vue'),
  <#elseif (node.nodeType?upper_case?contains('DETAIL'))>
  component: () => import('@/views/${componentDir}/Form.vue'),
  <#else>
  component: () => import('@/views/${componentDir}/Form.vue'),
  </#if>
      meta: { moduleCode: '${node.moduleCode}', nodeCode: '${node.nodeCode}', relatedTableCode: '${node.relatedTableCode}' }
    }<#if node?has_next>,</#if>
    </#if>
  </#list>
<#else>
  {
    path: '/${componentName?lower_case}/list',
    name: '${componentName}List',
    component: () => import('@/views/${componentDir}/List.vue'),
    meta: { relatedTableCode: '${table.tableCode}' }
  },
  {
    path: '/${componentName?lower_case}/form/:id?',
    name: '${componentName}Form',
    component: () => import('@/views/${componentDir}/Form.vue'),
    meta: { relatedTableCode: '${table.tableCode}' }
  }
</#if>
]
