# 通用元数据管理系统

## 项目简介

通用元数据管理系统是一套**无业务依赖的纯配置中心**，支持管理员通过可视化配置，定义抽象的模块、表结构、字段属性、业务规则，生成通用元数据模型。系统仅负责元数据的配置、存储和查询，不涉及任何具体业务场景。

## 技术栈

- **后端**: Java 17、Spring Boot 2.7.18、MyBatis、HikariCP、MySQL 8.0
- **前端**: Vue 3 + Element Plus
- **服务器**: Tomcat 9.0（部署在8080端口）
- **辅助**: Freemarker（用于元数据模板导出）

## 项目结构

```
meta-management/
├── src/main/java/com/metadata/          # 后端Java代码
│   ├── config/                          # 配置类
│   ├── controller/                      # 控制器
│   ├── entity/                          # 实体类
│   ├── mapper/                          # MyBatis Mapper接口
│   ├── service/                         # 业务逻辑层
│   ├── common/                          # 通用类
│   ├── interceptor/                     # 拦截器
│   └── util/                            # 工具类
├── src/main/resources/
│   ├── mapper/                          # MyBatis XML映射文件
│   └── application.yml                  # 应用配置
├── frontend/                            # 前端Vue项目
│   ├── src/
│   │   ├── api/                         # API接口
│   │   ├── views/                       # 页面组件
│   │   ├── router/                      # 路由配置
│   │   └── utils/                       # 工具类
│   └── package.json
├── database/
│   └── init.sql                         # 数据库初始化脚本
└── pom.xml                              # Maven配置
```

## 部署说明

### 1. 数据库初始化

1. 创建MySQL数据库：
```sql
CREATE DATABASE metadata_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本：
```bash
mysql -u root -p metadata_db < database/init.sql
```

3. 初始化管理员密码（密码：123456）：
   - 默认管理员用户名：`admin`
   - 密码需要BCrypt加密，可通过运行程序生成

### 2. 后端部署

1. 修改 `src/main/resources/application.yml` 中的数据库连接配置

2. 打包项目：
```bash
mvn clean package
```

3. 将生成的 `metadata-system.war` 部署到 Tomcat 9.0 的 `webapps` 目录

4. 启动 Tomcat，访问：`http://localhost:8080/metadata-system`

### 3. 前端部署

1. 进入前端目录：
```bash
cd frontend
```

2. 安装依赖：
```bash
npm install
```

3. 开发环境运行：
```bash
npm run serve
```

4. 生产环境打包：
```bash
npm run build
```

5. 将 `dist` 目录下的文件复制到后端的 `src/main/resources/static` 目录

## 功能模块

1. **模块管理**: 配置抽象模块，支持多种模块类型
2. **表管理**: 配置抽象表结构
3. **字段管理**: 配置表的字段属性
4. **功能节点**: 配置模块的功能节点
5. **业务规则**: 配置抽象业务规则（JSON格式）
6. **表关联**: 配置表之间的关联关系
7. **元数据导出**: 导出JSON格式的元数据
8. **操作日志**: 记录所有配置操作

## API接口

### 认证接口
- `POST /api/auth/login` - 登录
- `POST /api/auth/logout` - 退出
- `POST /api/auth/changePassword` - 修改密码

### 元数据查询接口
- `GET /api/metadata/module/{moduleCode}` - 查询模块详情
- `GET /api/metadata/table/{tableCode}/fields` - 查询表字段配置
- `GET /api/metadata/export/all` - 导出所有元数据

## 注意事项

1. 所有配置项均为抽象名称，不包含具体业务术语
2. 编码格式：字母、数字、下划线，长度1-50
3. 默认管理员密码：123456（首次登录后建议修改）
4. 系统支持跨域访问，可在配置文件中自定义允许的域名

## 开发约束

- 全程无具体业务绑定
- 代码逻辑通用，无硬编码业务逻辑
- 支持自定义扩展（模块类型、功能节点、校验规则）
- 安全性：SQL注入防护、密码加密、接口权限控制

