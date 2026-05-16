# 生成代码 — 开箱说明

> 本 README 由**元数据管理系统**自动生成。  
<#if projectZip?? && projectZip>
> **项目 ZIP** 已包含业务系统 **${businessName}**（`${businessCode}`）下 **${enabledTableCount}** 张启用表的完整后端 + 可运行前端。  
<#else>
> 随单表代码包导出；也可在代码生成页选择业务系统后使用 **「下载业务系统 ZIP」** 获取整包。  
> 业务系统：**${businessName}**（`${businessCode}`）  
> 当前表：**${tableName}**（表编码 `${tableCode}`）  
</#if>
> Java 包名：`${packageName}` · Maven `artifactId`：`${artifactId}` · JDK：**${javaVersion}**

---

## 1. 文件说明（界面平铺导出 vs ZIP）

| 文件 | 用途 |
|------|------|
| `create_table.sql` | 建表脚本<#if projectZip?? && projectZip>（ZIP 内 `database/schema.sql` 已合并全部启用表）</#if> |
| `*.java` / `*.xml` | Spring Boot + MyBatis 后端<#if projectZip?? && projectZip>（ZIP 已按表生成 Entity/Controller/Service/Mapper）</#if> |
| `*.vue`、`routes.js`、`api.js` | 前端<#if projectZip?? && projectZip>（ZIP 内 `frontend/` 为完整 Vite 工程）</#if> |
| `pom.xml` | 独立后端工程坐标，可与现有工程合并或新建工程使用 |
| `application.yml` | 数据源等配置（**已使用占位库名/密码，请修改**） |
| `.env` / `.env.example` | 前端环境变量；**将 `.env.example` 复制为 `.env`** 后按需改标题等 |

---

## 2. 后端：建议目录（以包 `${packageName}` 为例）

将 Java/XML 按包路径放入：

`src/main/java/${packagePath}/`

资源文件：

`src/main/resources/mapper/`（若有 `Mapper.xml`）

**推荐步骤：**

1. 安装 **JDK ${javaVersion}**、**Maven 3.8+**、**MySQL 8.x**。  
2. 在 MySQL 中创建数据库（名称建议与业务系统默认库一致，当前提示名：**`${databaseHint}`**；亦可自定，与 `application.yml` 中 `jdbc:mysql://.../` 后库名一致即可）。  
3. 执行 `create_table.sql` 建表。  
4. 编辑 `application.yml`：修改 `spring.datasource.url`、`username`、`password`。  
5. 在项目根目录执行：`mvn clean package` 后 `java -jar target/<artifactId>-0.0.1-SNAPSHOT.jar`，或使用 IDE 运行 `Application.java`。  
6. 默认端口见 `application.yml` 中 `server.port`（一般为 **8080**）。

<#if projectZip?? && projectZip && enabledTables??>
**各表 REST 前缀（`http://localhost:8080` + 下列路径）：**

| 表编码 | 表名 | API 前缀 |
|--------|------|----------|
<#list enabledTables as t>
| `${t.tableCode}` | ${t.tableName} | `${t.apiPrefix}` |
</#list>

<#else>
**本表 REST 路径前缀：**

后端根地址示例：`http://localhost:8080`，本资源接口前缀为 **`${apiPrefix}`**。
</#if>

<#if captchaEnabled>
---

## 3. 认证扩展（已勾选「字符型验证码」生成）

生成包内若包含 `AuthController.java`、`CaptchaService.java` 等，请与主包同目录放置，并保证 `application.yml` 中 `app.auth` / `app.captcha` 段存在（本包 `application.yml` 已带模板）。

默认测试账号以 `application.yml` 中 `app.auth.default-username` / `default-password` 为准（若存在）。

</#if>

---

## 4. 前端（ZIP 内已含可运行 Vite 工程）

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

浏览器打开 http://localhost:5173 。侧栏与路由来自元数据「功能节点」配置（`frontend/src/router/routes.js`）。`/api` 已在 `vite.config.js` 中代理到后端（默认 8080）。

---

## 5. 多表说明

<#if projectZip?? && projectZip>
本 ZIP 已包含该业务系统下全部**启用**表的 SQL、后端与前端页面。未启用的表不会打入包内；请在元数据「表管理」中启用后再重新下载。
<#else>
若还有其它表，请逐表生成或下载 **项目 ZIP**；公共类（如 `Result.java`）多表间保留一份即可。
</#if>

---

## 6. 免责声明（毕设 / 课设交付用）

生成代码仅供学习与二次开发；生产环境请自行补充权限、审计、备份与压测。
