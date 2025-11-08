# 部署指南

## 一、环境要求

- JDK 17
- Maven 3.6+
- MySQL 8.0
- Tomcat 9.0
- Node.js 16+ (前端开发环境)

## 二、数据库初始化

### 1. 创建数据库

```sql
CREATE DATABASE metadata_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 执行初始化脚本

```bash
mysql -u root -p metadata_db < database/init.sql
```

### 3. 验证管理员账号

- 用户名：`admin`
- 密码：`123456`

如果密码无法登录，可以运行以下命令生成新的BCrypt密码：

```bash
# 编译项目后运行
java -cp target/metadata-system-1.0.0.jar com.metadata.util.InitAdminUtil
```

然后将生成的密码更新到数据库：

```sql
UPDATE metadata_admin SET password = '生成的BCrypt密码' WHERE username = 'admin';
```

## 三、后端部署

### 1. 修改配置

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/metadata_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password
```

### 2. 编译打包

```bash
mvn clean package
```

生成的war文件位于：`target/metadata-system.war`

### 3. 部署到Tomcat

1. 将 `metadata-system.war` 复制到 Tomcat 的 `webapps` 目录
2. 启动 Tomcat
3. 访问：`http://localhost:8080/metadata-system`

## 四、前端部署

### 开发环境

```bash
cd frontend
npm install
npm run serve
```

访问：`http://localhost:8081`

### 生产环境

1. 打包前端：

```bash
cd frontend
npm install
npm run build
```

2. 将 `frontend/dist` 目录下的所有文件复制到后端的 `src/main/resources/static` 目录

3. 重新打包后端：

```bash
mvn clean package
```

4. 部署到Tomcat

## 五、验证部署

1. 访问：`http://localhost:8080/metadata-system`
2. 使用管理员账号登录：`admin` / `123456`
3. 检查各功能模块是否正常

## 六、常见问题

### 1. 登录失败

- 检查数据库连接配置
- 验证管理员密码是否正确（BCrypt加密）
- 查看后端日志

### 2. 前端页面404

- 确认前端文件已正确复制到 `static` 目录
- 检查 `vue.config.js` 中的 `publicPath` 配置

### 3. 跨域问题

- 检查 `application.yml` 中的跨域配置
- 确认前端代理配置正确

### 4. 数据库连接失败

- 检查MySQL服务是否启动
- 验证数据库用户名密码
- 确认数据库已创建

## 七、生产环境建议

1. 修改默认管理员密码
2. 配置HTTPS
3. 设置数据库连接池参数
4. 配置日志级别
5. 设置跨域白名单
6. 定期备份数据库

