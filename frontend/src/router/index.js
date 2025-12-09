import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import BusinessSystemManage from '../views/BusinessSystemManage.vue'
import ModuleManage from '../views/ModuleManage.vue'
import ModuleTypeManage from '../views/ModuleTypeManage.vue'
import TableManage from '../views/TableManage.vue'
import FieldManage from '../views/FieldManage.vue'
import NodeManage from '../views/NodeManage.vue'
import RuleManage from '../views/RuleManage.vue'
import RelationManage from '../views/RelationManage.vue'
import OperationLog from '../views/OperationLog.vue'
import CodeGenerator from '../views/CodeGenerator.vue'
import SqlExecute from '../views/SqlExecute.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/',
    component: Layout,
    redirect: '/module',
    children: [
      {
        path: '/business-system',
        name: 'BusinessSystemManage',
        component: BusinessSystemManage
      },
      {
        path: '/module',
        name: 'ModuleManage',
        component: ModuleManage
      },
      {
        path: '/module-type',
        name: 'ModuleTypeManage',
        component: ModuleTypeManage
      },
      {
        path: '/table',
        name: 'TableManage',
        component: TableManage
      },
      {
        path: '/field',
        name: 'FieldManage',
        component: FieldManage
      },
      {
        path: '/node',
        name: 'NodeManage',
        component: NodeManage
      },
      {
        path: '/rule',
        name: 'RuleManage',
        component: RuleManage
      },
      {
        path: '/relation',
        name: 'RelationManage',
        component: RelationManage
      },
      {
        path: '/log',
        name: 'OperationLog',
        component: OperationLog
      },
      {
        path: '/codegen',
        name: 'CodeGenerator',
        component: CodeGenerator
      },
      {
        path: '/sql',
        name: 'SqlExecute',
        component: SqlExecute
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory('/metadata-system/'),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('admin')
  if (to.path === '/login') {
    next()
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router

