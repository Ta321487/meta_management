# DEMO_ERP 全链路 — API 测试用例

> 对应 UI 剧本：[full-system-simulation-guide.md](./full-system-simulation-guide.md)  
> 在 **Swagger UI**（`/metadata-system/swagger-ui.html`）或 Postman 中按章节顺序调用。

---

## 0. 环境与约定

| 项 | 值 |
|----|-----|
| 服务根地址 | `http://localhost:8080/metadata-system` |
| API 前缀 | `/api` |
| 完整示例 | `http://localhost:8080/metadata-system/api/auth/login` |
| 认证方式 | **Session Cookie**（登录后自动带 `JSESSIONID`） |
| 测试账号 | `admin` / `123456` |
| 业务系统 | `DEMO_ERP` |
| 物理库名 | `demo_erp` |
| 包名 | `com.demo.erp` |

### 统一响应

```json
{ "code": 200, "message": "操作成功", "data": { } }
```

- `code === 200`：成功  
- `code === 9001`：未登录（需先调登录）  
- 其它四位业务码见 `AppErrorCodes`（如字段 `11xx`、物理库 `16xx`）

### Swagger / Postman 注意

1. 先调 **登录**，再在浏览器 Swagger 勾选 **Authorize** 或 Postman 开启 **Cookie 自动携带**。  
2. Postman 建议新建 Environment 变量：`baseUrl` = `http://localhost:8080/metadata-system/api`。  
3. 下列路径均省略 `baseUrl`，以 `/api/...` 表示。

---

## 1. 认证

### 1.1 登录

`POST /api/auth/login`

```json
{
  "username": "admin",
  "password": "123456"
}
```

**预期**：`code=200`，`data.username=admin`，响应头 `Set-Cookie: JSESSIONID=...`

### 1.2 退出（可选）

`POST /api/auth/logout`

### 1.3 修改密码（可选）

`POST /api/auth/changePassword`

```json
{
  "oldPassword": "123456",
  "newPassword": "你的新密码"
}
```

---

## 2. 库管理

### 2.1 新增库 `demo_erp`

`POST /api/physicalDatabase/add`

```json
{
  "catalogName": "demo_erp",
  "displayName": "演示ERP物理库",
  "description": "全链路模拟用物理库，与业务系统默认库一致",
  "charsetName": "utf8mb4",
  "collationName": "utf8mb4_unicode_ci",
  "syncToInstance": 1,
  "isEnabled": 1
}
```

### 2.2 列表 / 建库 / 更新

| 操作 | 方法 | 路径 |
|------|------|------|
| 列表 | GET | `/api/physicalDatabase/list` |
| 去服务器建库 | POST | `/api/physicalDatabase/sync/{id}` |
| 更新 | POST | `/api/physicalDatabase/update`（body 带 `id`，其余同新增） |

### 2.3 删除（模拟中一般跳过）

`DELETE /api/physicalDatabase/delete/{id}?dropOnInstance=false`

- `dropOnInstance=false`：只删元数据配置  
- `dropOnInstance=true`：元数据删完后 `DROP DATABASE`（慎用）

---

## 3. 模块类型（3 条）

`POST /api/moduleType/add`

**类型 A**

```json
{
  "typeCode": "MASTER_DATA",
  "typeName": "主数据模块类型",
  "defaultNodes": ["列表页", "表单页", "详情页"],
  "description": "客户、商品、仓库等主数据维护类模块使用该类型"
}
```

**类型 B**：`typeCode=ORDER`，`typeName=订单业务模块类型`，`defaultNodes=["列表页","表单页","流程页","批量导出页"]`

**类型 C**：`typeCode=REPORT`，`typeName=报表与配置模块类型`，`defaultNodes=["报表页","列表页","批量导入页","批量导出页"]`

查询：`GET /api/moduleType/list`

---

## 4. 业务系统

`POST /api/businessSystem/add`

```json
{
  "businessCode": "DEMO_ERP",
  "businessName": "演示ERP业务系统",
  "packageName": "com.demo.erp",
  "databaseName": "demo_erp",
  "description": "全链路模拟：主数据、销售、仓储、系统配置共八大表",
  "isDefault": 1
}
```

| 操作 | 方法 | 路径 |
|------|------|------|
| 列表 | GET | `/api/businessSystem/list` |
| 详情 | GET | `/api/businessSystem/DEMO_ERP` |
| 默认 | GET | `/api/businessSystem/default` |

---

## 5. 模块（4 条）

`POST /api/module/add` — 示例 **MDM**（`tableCodes` 可先 `[]`，建表后再 update 补全）

```json
{
  "moduleCode": "MDM",
  "moduleName": "主数据管理",
  "moduleType": "MASTER_DATA",
  "businessCode": "DEMO_ERP",
  "description": "客户、商品、仓库主数据",
  "tableCodes": [],
  "sort": 10,
  "icon": "User",
  "routePath": "/mdm",
  "componentPath": "views/mdm",
  "status": 1
}
```

其余模块按剧本：`SALES`、`WH`、`CFG`（字段见 UI 剧本第 5 节）。

| 操作 | 方法 | 路径 |
|------|------|------|
| 列表 | GET | `/api/module/list?businessCode=DEMO_ERP` |
| 详情 | GET | `/api/module/MDM` |
| 重建功能节点 | POST | `/api/module/rebuildNodes/MDM` |

---

## 6. 业务系统关联模块

`POST /api/businessSystem/associateModules`

```json
{
  "businessCode": "DEMO_ERP",
  "moduleCodes": ["MDM", "SALES", "WH", "CFG"]
}
```

查询已关联：`GET /api/businessSystem/associatedModules/DEMO_ERP`

---

## 7. 表（8 张）

`POST /api/table/add` — 示例 **mdm_customer**（`databaseName` 留空则用业务系统默认库）

```json
{
  "tableCode": "mdm_customer",
  "tableName": "客户主数据表",
  "pkStrategy": "AUTO",
  "description": "客户编码、名称、状态及时间戳",
  "businessCode": "DEMO_ERP",
  "databaseName": null,
  "isEnabled": 1
}
```

八张表编码（剧本第 7 节）：

`mdm_customer` · `mdm_product` · `mdm_warehouse` · `sales_order` · `sales_order_line` · `wh_stock` · `sys_dict` · `sys_audit_log`

| 操作 | 方法 | 路径 |
|------|------|------|
| 列表 | GET | `/api/table/list?businessCode=DEMO_ERP` |
| 详情 | GET | `/api/table/mdm_customer` |
| 按模块 | GET | `/api/table/listByModule/MDM?businessCode=DEMO_ERP` |
| 批量分配业务系统 | POST | `/api/table/batchAssignBusinessSystem`（body：`tableCodes` + `businessCode`） |

---

## 8. 补全模块关联表

`POST /api/module/update` — 以 MDM 为例：

```json
{
  "id": 1,
  "moduleCode": "MDM",
  "moduleName": "主数据管理",
  "moduleType": "MASTER_DATA",
  "businessCode": "DEMO_ERP",
  "tableCodes": ["mdm_customer", "mdm_product", "mdm_warehouse"],
  "sort": 10,
  "icon": "User",
  "routePath": "/mdm",
  "componentPath": "views/mdm",
  "status": 1
}
```

---

## 9. 字段

`POST /api/field/add` — 示例 **mdm_customer** 客户编码行：

```json
{
  "fieldCode": "MC_F_CODE",
  "tableCode": "mdm_customer",
  "fieldName": "customer_code",
  "fieldType": "VARCHAR(32)",
  "label": "客户编码",
  "isRequired": 1,
  "formComponent": "input",
  "validateRule": "{\"min\":1,\"max\":32,\"message\":\"客户编码长度1-32\",\"trigger\":\"blur\"}",
  "sort": 10,
  "isEnabled": 1,
  "businessCode": "DEMO_ERP"
}
```

> 新增表时系统会自动建主键字段 `id`；其余字段按剧本第 9 节逐表录入。

| 操作 | 方法 | 路径 |
|------|------|------|
| 列表 | GET | `/api/field/list/mdm_customer?current=1&size=20` |
| 约束列表 | GET | `/api/field/constraint/list/mdm_customer` |

---

## 10. 表关联

`POST /api/relation/add` — **关联 1**（订单头 ↔ 明细，剧本勾选外键）

```json
{
  "relationCode": "REL_SO_LINE_001",
  "relationName": "订单头与订单明细一对多",
  "mainTableCode": "sales_order",
  "mainFieldCode": "SO_F_ID",
  "slaveTableCode": "sales_order_line",
  "slaveFieldCode": "SOL_F_OID",
  "relationType": "ONE_TO_MANY",
  "description": "明细.order_id 指向订单头.id",
  "businessCode": "DEMO_ERP"
}
```

创建外键：`POST /api/relation/createForeignKey`（body 同上关联对象）

同步外键：`POST /api/relation/syncForeignKeys` 或 `POST /api/relation/syncForeignKeys?tableCode=sales_order_line`

列表：`GET /api/relation/list?businessCode=DEMO_ERP`

---

## 11. 功能节点

`POST /api/node/add` — 示例 **MDM 客户列表**

```json
{
  "nodeCode": "MDM_NODE_CUST_LIST",
  "nodeName": "客户列表",
  "moduleCode": "MDM",
  "nodeType": "LIST_PAGE",
  "relatedTableCode": "mdm_customer",
  "jumpRelation": "MDM_NODE_CUST_LIST→MDM_NODE_CUST_FORM",
  "routePath": "/customer/list",
  "componentPath": "CustomerList.vue",
  "isMenuVisible": 1,
  "icon": "User",
  "sort": 10,
  "isEnabled": 1,
  "businessCode": "DEMO_ERP"
}
```

列表：`GET /api/node/list/MDM?businessCode=DEMO_ERP`

---

## 12. 业务规则

`POST /api/rule/add` — 示例 **MDM 唯一校验**

```json
{
  "ruleCode": "RULE_MDM_VAL_001",
  "moduleCode": "MDM",
  "ruleType": "VALIDATION_RULE",
  "ruleContent": "{\"field\":\"customer_code\",\"type\":\"unique\",\"message\":\"客户编码已存在\"}",
  "description": "客户编码唯一校验",
  "businessCode": "DEMO_ERP"
}
```

列表：`GET /api/rule/list/MDM?businessCode=DEMO_ERP`

---

## 13. SQL 执行

`POST /api/sql/execute`

**探测**

```json
{
  "sql": "SELECT 1 AS probe_connection, 'DEMO_ERP' AS business_code, DATABASE() AS current_db"
}
```

**演示数据**（块 3，单条示例）

```json
{
  "sql": "INSERT INTO mdm_customer (customer_code, customer_name, status, remark) VALUES ('CUST0001','华东贸易有限公司',1,'模拟客户')"
}
```

> 八表 `CREATE TABLE` 全文见 UI 剧本第 13 节；可用 `POST /api/sql/executeMultiple` 传多语句。系统禁止 `DROP` 类语句。

---

## 14. 代码生成（抽查）

| 说明 | 方法 | 路径 |
|------|------|------|
| 建表 SQL | GET | `/api/codegen/sql/mdm_customer?businessCode=DEMO_ERP` |
| 实体类 | GET | `/api/codegen/entity/mdm_customer?packageName=com.demo.erp.entity&businessCode=DEMO_ERP` |
| 全量 | GET | `/api/codegen/all/mdm_customer?packageName=com.demo.erp&businessCode=DEMO_ERP&useInterface=false` |
| 按业务系统 | GET | `/api/codegen/allByBusinessSystem/DEMO_ERP?packageName=com.demo.erp` |
| 代码测试 | GET | `/api/codetest/test/mdm_customer?packageName=com.demo.erp&businessCode=DEMO_ERP` |

---

## 15. 操作日志

`GET /api/operationLog/list?current=1&size=20`

---

## 16. 元数据导出（可选）

| 说明 | 方法 | 路径 |
|------|------|------|
| 模块元数据 | GET | `/api/metadata/module/MDM` |
| 表字段 | GET | `/api/metadata/table/mdm_customer/fields` |
| 全量导出 | GET | `/api/metadata/export/all` |

---

## 附录 A — 建议调用顺序（Checklist）

```
[ ] POST /api/auth/login
[ ] POST /api/physicalDatabase/add
[ ] POST /api/moduleType/add ×3
[ ] POST /api/businessSystem/add
[ ] POST /api/module/add ×4
[ ] POST /api/businessSystem/associateModules
[ ] POST /api/table/add ×8
[ ] POST /api/module/update ×4（补 tableCodes）
[ ] POST /api/field/add（各表字段，或依赖建表自动同步）
[ ] POST /api/relation/add ×4 + createForeignKey（关联1）
[ ] POST /api/node/add ×8
[ ] POST /api/rule/add ×6
[ ] POST /api/sql/execute（探测 + 演示数据）
[ ] GET  /api/codegen/all/mdm_customer
[ ] GET  /api/operationLog/list
```

---

## 附录 B — cURL 快速登录示例

```bash
curl -c cookies.txt -X POST "http://localhost:8080/metadata-system/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\"}"

curl -b cookies.txt "http://localhost:8080/metadata-system/api/businessSystem/list"
```

---

## 附录 C — 常见错误码（测试断言）

| code | 含义 |
|------|------|
| 200 | 成功 |
| 9001 | 未登录 |
| 1001 | 表不存在 |
| 1102 | 字段不存在 |
| 1103 | 字段编码重复 |
| 1601 | 物理库记录不存在 |
| 1603 | 库名已存在 |
| 1604 | 删库级联失败 |
| 9999 | 系统内部错误 |
