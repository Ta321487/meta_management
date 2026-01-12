<template>
  <!-- 部署指南对话框 -->
  <el-dialog
    v-model="localVisible"
    title="代码部署指南"
    width="1000px"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
  >
    <el-tabs v-model="activeGuideTab" type="border-card">
      <!-- 后端部署指南 -->
      <el-tab-pane label="后端部署" name="backend">
        <div class="deployment-guide">
          <h3>1. 项目结构创建</h3>
          <div class="file-tree">
            <pre>
项目根目录/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/          <!-- 你的包名 -->
│   │   │           ├── common/       <!-- 通用工具类 -->
│   │   │           ├── config/        <!-- 配置类 -->
│   │   │           ├── controller/    <!-- 控制器 -->
│   │   │           ├── entity/        <!-- 实体类 -->
│   │   │           ├── mapper/        <!-- Mapper接口 -->
│   │   │           ├── service/       <!-- 服务类 -->
│   │   │           └── Application.java  <!-- 启动类 -->
│   │   └── resources/
│   │       ├── mapper/               <!-- Mapper XML文件 -->
│   │       └── application.yml       <!-- 配置文件 -->
│   └── test/                          <!-- 测试目录 -->
└── pom.xml                           <!-- Maven依赖配置 -->
            </pre>
          </div>

          <h3>2. 文件放置位置</h3>
          <table class="file-placement-table">
            <thead>
              <tr>
                <th>生成的文件</th>
                <th>目标位置</th>
                <th>说明</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Entity.java</td>
                <td>src/main/java/your/package/entity/</td>
                <td>数据实体类</td>
              </tr>
              <tr>
                <td>Controller.java</td>
                <td>src/main/java/your/package/controller/</td>
                <td>API控制器</td>
              </tr>
              <tr>
                <td>Service.java</td>
                <td>src/main/java/your/package/service/</td>
                <td>业务逻辑层</td>
              </tr>
              <tr>
                <td>Mapper.java</td>
                <td>src/main/java/your/package/mapper/</td>
                <td>数据访问接口</td>
              </tr>
              <tr>
                <td>Mapper.xml</td>
                <td>src/main/resources/mapper/</td>
                <td>MyBatis映射文件</td>
              </tr>
              <tr>
                <td>Application.java</td>
                <td>src/main/java/your/package/</td>
                <td>Spring Boot启动类</td>
              </tr>
              <tr>
                <td>Application.yml</td>
                <td>src/main/resources/</td>
                <td>Spring Boot配置文件</td>
              </tr>
              <tr>
                <td>Pom.xml</td>
                <td>项目根目录</td>
                <td>Maven依赖配置</td>
              </tr>
              <tr>
                <td>Result.java</td>
                <td>src/main/java/your/package/common/</td>
                <td>统一响应结果类</td>
              </tr>
              <tr>
                <td>PageRequest.java</td>
                <td>src/main/java/your/package/common/</td>
                <td>分页请求类</td>
              </tr>
              <tr>
                <td>PageResult.java</td>
                <td>src/main/java/your/package/common/</td>
                <td>分页结果类</td>
              </tr>
              <tr>
                <td>MyBatisConfig.java</td>
                <td>src/main/java/your/package/config/</td>
                <td>MyBatis配置类</td>
              </tr>
            </tbody>
          </table>
        </div>
      </el-tab-pane>

      <!-- 前端部署指南 -->
      <el-tab-pane label="前端部署" name="frontend">
        <div class="deployment-guide">
          <h3>1. 项目结构创建</h3>
          <div class="file-tree">
            <pre>
项目根目录/
├── public/                           <!-- 静态资源目录 -->
├── src/
│   ├── assets/                       <!-- 资源文件 -->
│   ├── components/                   <!-- 组件 -->
│   ├── views/                        <!-- 页面视图 -->
│   ├── router/                       <!-- 路由配置 -->
│   ├── api/                          <!-- API请求 -->
│   ├── utils/                        <!-- 工具函数 -->
│   ├── App.vue                       <!-- 根组件 -->
│   └── main.js                       <!-- 入口文件 -->
├── .env                              <!-- 环境配置 -->
├── .env.development                  <!-- 开发环境配置 -->
├── .env.production                   <!-- 生产环境配置 -->
├── package.json                      <!-- 依赖配置 -->
└── vite.config.js                    <!-- Vite配置 -->
            </pre>
          </div>

          <h3>2. 文件放置位置</h3>
          <table class="file-placement-table">
            <thead>
              <tr>
                <th>生成的文件</th>
                <th>目标位置</th>
                <th>说明</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>List.vue</td>
                <td>src/views/</td>
                <td>列表页面</td>
              </tr>
              <tr>
                <td>Form.vue</td>
                <td>src/views/</td>
                <td>表单页面</td>
              </tr>
              <tr>
                <td>Login.vue</td>
                <td>src/views/</td>
                <td>登录页面</td>
              </tr>
              <tr>
                <td>routes.js</td>
                <td>src/router/</td>
                <td>路由配置</td>
              </tr>
              <tr>
                <td>api.js</td>
                <td>src/api/</td>
                <td>API请求文件</td>
              </tr>
              <tr>
                <td>auth.js</td>
                <td>src/api/</td>
                <td>认证API文件</td>
              </tr>
              <tr>
                <td>request.js</td>
                <td>src/utils/</td>
                <td>请求工具类</td>
              </tr>
              <tr>
                <td>.env</td>
                <td>项目根目录</td>
                <td>环境配置文件</td>
              </tr>
            </tbody>
          </table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue';

// Props
const props = defineProps({
  // 可见性
  visible: {
    type: Boolean,
    default: false
  }
});

// Emits
const emit = defineEmits(['update:visible']);

// Data
// 当前激活的指南标签
const activeGuideTab = ref('backend');
// 本地可见性变量
const localVisible = ref(props.visible);

// Watch
// 监听外部可见性变化
watch(() => props.visible, (newVal) => {
  localVisible.value = newVal;
});

// 监听内部可见性变化
watch(() => localVisible.value, (newVal) => {
  emit('update:visible', newVal);
});
</script>

<style scoped>
.deployment-guide {
  padding: 20px 0;
}

h3 {
  margin: 20px 0 10px 0;
  color: #303133;
  font-size: 16px;
  font-weight: bold;
}

.file-tree {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  overflow-x: auto;
}

.file-tree pre {
  margin: 0;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  color: #303133;
}

.file-placement-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}

.file-placement-table th,
.file-placement-table td {
  padding: 10px;
  border: 1px solid #ebeef5;
  text-align: left;
}

.file-placement-table th {
  background-color: #f5f7fa;
  font-weight: bold;
  color: #303133;
}

.file-placement-table td {
  color: #606266;
}
</style>
