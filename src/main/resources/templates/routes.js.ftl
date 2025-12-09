<#-- 生成前端路由配置（routes.js） -->
<#-- data: table, componentName, componentDir, nodes -->
export default [
<#if nodes?has_content>
  <#list nodes as node>
    <#-- 对已配置 routePath 或 jumpRelation 的节点生成路由 -->
    <#if node.routePath?has_content || node.jumpRelation?has_content>
    {
      <#-- 优先使用 routePath，否则使用 jumpRelation -->
      path: '/${businessCode}${node.routePath?has_content?then(node.routePath, node.jumpRelation)}',
      name: '${node.nodeCode}',
      <#-- 根据 nodeType 选择组件名 -->
      <#-- 支持 nodeType 包含 LIST、FORM、DETAIL 等关键词 -->
      <#if (node.nodeType?upper_case?contains('LIST'))>
        <#-- 优先使用 componentPath，否则使用默认路径 -->
        component: () => import('@/views/${componentDir}/${node.componentPath?has_content?then(node.componentPath, 'List.vue')}'),
      <#elseif (node.nodeType?upper_case?contains('FORM'))>
        component: () => import('@/views/${componentDir}/${node.componentPath?has_content?then(node.componentPath, 'Form.vue')}'),
      <#elseif (node.nodeType?upper_case?contains('DETAIL'))>
        component: () => import('@/views/${componentDir}/${node.componentPath?has_content?then(node.componentPath, 'Form.vue')}'),
      <#else>
        component: () => import('@/views/${componentDir}/${node.componentPath?has_content?then(node.componentPath, 'Form.vue')}'),
      </#if>
      meta: { 
        moduleCode: '${node.moduleCode}', 
        nodeCode: '${node.nodeCode}', 
        relatedTableCode: '${node.relatedTableCode}',
        <#-- 添加菜单显示配置 -->
        isMenuVisible: ${node.isMenuVisible!1},
        <#-- 添加图标配置 -->
        icon: '${node.icon!''}'
      }
    }<#if node?has_next>,</#if>
    </#if>
  </#list>
<#else>
  {
    path: '/${businessCode}/${componentName?lower_case}/list',
    name: '${componentName}List',
    component: () => import('@/views/${componentDir}/List.vue'),
    meta: { relatedTableCode: '${table.tableCode}', isMenuVisible: 1, businessCode: '${businessCode}' }
  },
  {
    path: '/${businessCode}/${componentName?lower_case}/form/:id?',
    name: '${componentName}Form',
    component: () => import('@/views/${componentDir}/Form.vue'),
    meta: { relatedTableCode: '${table.tableCode}', isMenuVisible: 0, businessCode: '${businessCode}' }
  }
</#if>
]
