# 元数据管理系统 — 全链路模拟剧本（完整数据版）

> 约定：业务系统编码 **`DEMO_ERP`**；物理库名 **`demo_erp`**；包名 **`com.demo.erp`**。  
> 登录：用户名 **`admin`**，密码 **`123456`**（若库中已修改，以实际为准）。

---

## 目录

1. [登录与欢迎](#1-登录与欢迎)
2. [库管理](#2-库管理)
3. [模块类型管理](#3-模块类型管理)
4. [业务系统管理](#4-业务系统管理)
5. [模块管理](#5-模块管理)
6. [业务系统关联模块](#6-业务系统关联模块)
7. [表管理](#7-表管理)
8. [模块管理补全关联表](#8-模块管理补全关联表)
9. [字段管理](#9-字段管理)
10. [表关联管理](#10-表关联管理)
11. [功能节点管理](#11-功能节点管理)
12. [业务规则管理](#12-业务规则管理)
13. [SQL 执行](#13-sql-执行)
14. [代码生成](#14-代码生成)
15. [操作日志](#15-操作日志)
16. [侧栏与标签页](#16-侧栏与标签页)

---

## 1. 登录与欢迎

**路径**：`/login`

| 步骤 | 操作 |
|------|------|
| 1 | 「用户名」输入 `admin` |
| 2 | 「密码」输入 `123456`（或你的实际密码） |
| 3 | 点击「登录」 |
| 4 | 左侧菜单进入「欢迎」或访问 `/welcome`，浏览一次 |

**右上角下拉**（可选覆盖账号能力）：

- 「修改密码」：原密码 `123456`，新密码自定，登出后用新密码再登录一次  
- 或「退出登录」

---

## 2. 库管理

**页面**：库管理 → 「新增库」

### 对话框字段（逐项）

| 字段 | 填写值 |
|------|--------|
| 库名 `catalogName` | `demo_erp` |
| 展示名称 `displayName` | `演示ERP物理库` |
| 说明 `description` | `全链路模拟用物理库登记，与业务系统默认库一致` |
| 字符集 `charsetName` | `utf8mb4（推荐）` |
| 排序规则 `collationName` | `utf8mb4_unicode_ci（推荐）` |
| 保存时建库 `syncToInstance` | 开（1） |
| 启用 `isEnabled` | 开（1） |

### 列表行操作

1. 「编辑」→ 可直接确定，或改说明后再保存  
2. 「去服务器建库」  
3. 再次「编辑」，说明改为：`全链路模拟用物理库登记（已尝试建库）` → 保存  

删除库时：默认只清理元数据配置（会解除业务系统默认库引用，并删除「表级物理库名」指向该库的所有表元数据）；界面可选**同时删服务器库**，需二次确认后执行 `DROP DATABASE`。模拟流程中一般**不要删** `demo_erp`，避免后续步骤断档。

---

## 3. 模块类型管理

共 **3** 条，每条「新增类型」后保存。

### 类型 A

| 字段 | 值 |
|------|-----|
| 类型编码 `typeCode` | `MASTER_DATA` |
| 类型名称 `typeName` | `主数据模块类型` |
| 默认节点 `defaultNodes` | 多选：`列表页`、`表单页`、`详情页` |
| 描述 `description` | `客户、商品、仓库等主数据维护类模块使用该类型` |

### 类型 B

| 字段 | 值 |
|------|-----|
| 类型编码 | `ORDER` |
| 类型名称 | `订单业务模块类型` |
| 默认节点 | 多选：`列表页`、`表单页`、`流程页`、`批量导出页` |
| 描述 | `销售订单、订单明细及流程类页面使用该类型` |

### 类型 C

| 字段 | 值 |
|------|-----|
| 类型编码 | `REPORT` |
| 类型名称 | `报表与配置模块类型` |
| 默认节点 | 多选：`报表页`、`列表页`、`批量导入页`、`批量导出页` |
| 描述 | `字典、审计日志及报表配置类模块使用该类型` |

---

## 4. 业务系统管理

### 4.1 新增业务系统

| 字段 | 值 |
|------|-----|
| 业务编码 `businessCode` | `DEMO_ERP` |
| 业务系统名称 `businessName` | `演示ERP业务系统` |
| 包名 `packageName` | `com.demo.erp` |
| 默认物理库 `databaseName` | `demo_erp` |
| 描述 `description` | `全链路模拟：主数据、销售、仓储、系统配置共八大表` |
| 是否默认 `isDefault` | 是（1） |

### 4.2 列表操作

- 「编辑」核对后确定  
- 「设为默认」：已是默认则禁用可跳过  
- 勿「删除」

---

## 5. 模块管理

先新增 **4** 个模块；「关联表」可先空，待 [第 7 节](#7-表管理) 建表后在 [第 8 节](#8-模块管理补全关联表) 补全。

### 模块 1 — MDM

| 字段 | 值 |
|------|-----|
| 模块编码 `moduleCode` | `MDM` |
| 模块名称 `moduleName` | `主数据管理` |
| 模块类型 `moduleType` | `MASTER_DATA` |
| 业务系统 `businessCode` | `DEMO_ERP` |
| 描述 `description` | `客户、商品、仓库主数据` |
| 关联表 `tableCodes`（后补） | `mdm_customer`、`mdm_product`、`mdm_warehouse` |
| 排序号 `sort` | `10` |
| 图标 `icon` | `User` |
| 路由路径 `routePath` | `/mdm` |
| 组件路径 `componentPath` | `views/mdm` |

### 模块 2 — SALES

| 字段 | 值 |
|------|-----|
| 模块编码 | `SALES` |
| 模块名称 | `销售管理` |
| 模块类型 | `ORDER` |
| 业务系统 | `DEMO_ERP` |
| 描述 | `销售订单与明细` |
| 关联表（后补） | `sales_order`、`sales_order_line` |
| 排序号 | `20` |
| 图标 | `ShoppingCart` |
| 路由路径 | `/sales` |
| 组件路径 | `views/sales` |

### 模块 3 — WH

| 字段 | 值 |
|------|-----|
| 模块编码 | `WH` |
| 模块名称 | `仓储管理` |
| 模块类型 | `ORDER` |
| 业务系统 | `DEMO_ERP` |
| 描述 | `库存结存` |
| 关联表（后补） | `wh_stock` |
| 排序号 | `30` |
| 图标 | `Box` |
| 路由路径 | `/wh` |
| 组件路径 | `views/wh` |

### 模块 4 — CFG

| 字段 | 值 |
|------|-----|
| 模块编码 | `CFG` |
| 模块名称 | `系统配置` |
| 模块类型 | `REPORT` |
| 业务系统 | `DEMO_ERP` |
| 描述 | `数据字典与审计` |
| 关联表（后补） | `sys_dict`、`sys_audit_log` |
| 排序号 | `40` |
| 图标 | `Setting` |
| 路由路径 | `/cfg` |
| 组件路径 | `views/cfg` |

### 其它操作（覆盖列表能力）

- 勾选任意 2 行 → 「批量启用」  
- 勾选另 2 行 → 「批量禁用」→ 再逐行「启用」恢复全部为启用  
- 搜索：模块名称 `主数据`；模块类型 `主数据模块类型`；业务系统 `演示ERP业务系统`；状态「启用」→ 清空再加载  
- 对 `MDM` 点「重建功能节点」（建表前后各一次更佳）  
- 分页：每页 `20` → `10`，翻页  

---

## 6. 业务系统关联模块

业务系统管理 → `DEMO_ERP` 行 → 「关联模块」→ 勾选 **`MDM`、`SALES`、`WH`、`CFG`** → 确定。

---

## 7. 表管理

**搜索**：表名称 `mdm` + 业务系统筛选 → 重置。

每张表「新增表」：**业务系统**均选 `演示ERP业务系统`；**物理库名**一律留空（沿用默认库 `demo_erp`）。

| 序号 | 表编码 `tableCode` | 表名称 `tableName` | 主键策略 `pkStrategy` | 描述 `description` |
|------|-------------------|-------------------|----------------------|-------------------|
| 1 | `mdm_customer` | `客户主数据表` | `AUTO` | `客户编码、名称、状态及时间戳` |
| 2 | `mdm_product` | `商品主数据表` | `AUTO` | `商品编码、名称、状态及时间戳` |
| 3 | `mdm_warehouse` | `仓库主数据表` | `AUTO` | `仓库编码、名称、状态及时间戳` |
| 4 | `sales_order` | `销售订单头表` | `AUTO` | `订单号、客户、状态及时间戳` |
| 5 | `sales_order_line` | `销售订单明细表` | `AUTO` | `订单行号、商品、数量、金额` |
| 6 | `wh_stock` | `库存结存表` | `AUTO` | `仓库+商品维度结存` |
| 7 | `sys_dict` | `数据字典表` | `AUTO` | `字典类型与键值` |
| 8 | `sys_audit_log` | `业务审计日志表` | `AUTO` | `业务表、主键、动作、操作人` |

### 表列表其它操作

- 勾选 `mdm_customer`、`mdm_product` → 「批量分配业务系统」→ `DEMO_ERP`  
- 对 `sys_dict`：「禁用」→「启用」  
- 勿「批量删除」  

---

## 8. 模块管理补全关联表

分别「编辑」`MDM`、`SALES`、`WH`、`CFG`，按第 5 节表格补全「关联表」多选并保存。

---

## 9. 字段管理

**前提**：业务系统选 `演示ERP业务系统`；表下拉依次切换；每张表 **6** 条字段。

**通用**：

- 业务系统 `businessCode`：一律 `DEMO_ERP`  
- 未单独说明的 `validateRule`：JSON 填 `{}`  
- `id` 行：`isRequired` = 是，`formComponent` = `primary_key`  

列说明：**字段编码** | **字段名称** | **基础类型与参数** | **完整 fieldType** | **显示名** | **必填** | **表单组件** | **校验规则** | **排序**

### 表 `mdm_customer`

| fieldCode | fieldName | 类型与参数 | fieldType | label | 必填 | formComponent | validateRule | sort |
|-----------|-----------|------------|-----------|-------|------|---------------|--------------|------|
| `MC_F_ID` | `id` | BIGINT | `BIGINT` | `主键` | 是 | `primary_key` | `{}` | `0` |
| `MC_F_CODE` | `customer_code` | VARCHAR 长度 32 | `VARCHAR(32)` | `客户编码` | 是 | `input` | `{"min":1,"max":32,"message":"客户编码长度1-32","trigger":"blur"}` | `10` |
| `MC_F_NAME` | `customer_name` | VARCHAR 128 | `VARCHAR(128)` | `客户名称` | 是 | `input` | `{"min":1,"max":128,"message":"客户名称长度1-128","trigger":"blur"}` | `20` |
| `MC_F_STATUS` | `status` | TINYINT | `TINYINT` | `状态` | 是 | `number` | `{"type":"number","min":0,"max":2,"message":"状态0草稿1生效2作废","trigger":"blur"}` | `30` |
| `MC_F_REMARK` | `remark` | VARCHAR 512 | `VARCHAR(512)` | `备注` | 否 | `textarea` | `{}` | `40` |
| `MC_F_CT` | `create_time` | DATETIME | `DATETIME` | `创建时间` | 否 | `datepicker` | `{}` | `50` |

### 表 `mdm_product`

| fieldCode | fieldName | 类型与参数 | fieldType | label | 必填 | formComponent | validateRule | sort |
|-----------|-----------|------------|-----------|-------|------|---------------|--------------|------|
| `MP_F_ID` | `id` | BIGINT | `BIGINT` | `主键` | 是 | `primary_key` | `{}` | `0` |
| `MP_F_CODE` | `product_code` | VARCHAR 64 | `VARCHAR(64)` | `商品编码` | 是 | `input` | `{"min":1,"max":64,"message":"商品编码长度1-64","trigger":"blur"}` | `10` |
| `MP_F_NAME` | `product_name` | VARCHAR 256 | `VARCHAR(256)` | `商品名称` | 是 | `input` | `{}` | `20` |
| `MP_F_STATUS` | `status` | TINYINT | `TINYINT` | `状态` | 是 | `number` | `{"type":"number","min":0,"max":2,"message":"状态0-2","trigger":"blur"}` | `30` |
| `MP_F_REMARK` | `remark` | VARCHAR 512 | `VARCHAR(512)` | `备注` | 否 | `textarea` | `{}` | `40` |
| `MP_F_CT` | `create_time` | DATETIME | `DATETIME` | `创建时间` | 否 | `datepicker` | `{}` | `50` |

### 表 `mdm_warehouse`

| fieldCode | fieldName | 类型与参数 | fieldType | label | 必填 | formComponent | validateRule | sort |
|-----------|-----------|------------|-----------|-------|------|---------------|--------------|------|
| `MW_F_ID` | `id` | BIGINT | `BIGINT` | `主键` | 是 | `primary_key` | `{}` | `0` |
| `MW_F_CODE` | `warehouse_code` | VARCHAR 32 | `VARCHAR(32)` | `仓库编码` | 是 | `input` | `{}` | `10` |
| `MW_F_NAME` | `warehouse_name` | VARCHAR 128 | `VARCHAR(128)` | `仓库名称` | 是 | `input` | `{}` | `20` |
| `MW_F_STATUS` | `status` | TINYINT | `TINYINT` | `状态` | 是 | `number` | `{}` | `30` |
| `MW_F_REMARK` | `remark` | VARCHAR 512 | `VARCHAR(512)` | `备注` | 否 | `textarea` | `{}` | `40` |
| `MW_F_CT` | `create_time` | DATETIME | `DATETIME` | `创建时间` | 否 | `datepicker` | `{}` | `50` |

### 表 `sales_order`

| fieldCode | fieldName | 类型与参数 | fieldType | label | 必填 | formComponent | validateRule | sort |
|-----------|-----------|------------|-----------|-------|------|---------------|--------------|------|
| `SO_F_ID` | `id` | BIGINT | `BIGINT` | `主键` | 是 | `primary_key` | `{}` | `0` |
| `SO_F_NO` | `order_no` | VARCHAR 32 | `VARCHAR(32)` | `订单号` | 是 | `input` | `{}` | `10` |
| `SO_F_CID` | `customer_id` | BIGINT | `BIGINT` | `客户ID` | 是 | `number` | `{}` | `20` |
| `SO_F_ST` | `order_status` | ENUM：`DRAFT,CONFIRMED,CANCELLED`，点「刷新」同步校验 | `ENUM('DRAFT','CONFIRMED','CANCELLED')` | `订单状态` | 是 | `select` | `{"operator":"IN","values":["DRAFT","CONFIRMED","CANCELLED"],"message":"无效状态","trigger":"change"}` | `30` |
| `SO_F_CT` | `create_time` | DATETIME | `DATETIME` | `创建时间` | 否 | `datepicker` | `{}` | `40` |
| `SO_F_UT` | `update_time` | DATETIME | `DATETIME` | `更新时间` | 否 | `datepicker` | `{}` | `50` |

### 表 `sales_order_line`

| fieldCode | fieldName | 类型与参数 | fieldType | label | 必填 | formComponent | validateRule | sort |
|-----------|-----------|------------|-----------|-------|------|---------------|--------------|------|
| `SOL_F_ID` | `id` | BIGINT | `BIGINT` | `主键` | 是 | `primary_key` | `{}` | `0` |
| `SOL_F_OID` | `order_id` | BIGINT | `BIGINT` | `订单头ID` | 是 | `number` | `{}` | `10` |
| `SOL_F_LNO` | `line_no` | INT | `INT` | `行号` | 是 | `number` | `{"type":"number","integer":true,"min":1,"message":"行号>=1","trigger":"blur"}` | `20` |
| `SOL_F_PID` | `product_id` | BIGINT | `BIGINT` | `商品ID` | 是 | `number` | `{}` | `30` |
| `SOL_F_QTY` | `qty` | DECIMAL 14,4 | `DECIMAL(14,4)` | `数量` | 是 | `number` | `{"type":"number","min":0.0001,"message":"数量>0","trigger":"blur"}` | `40` |
| `SOL_F_AMT` | `line_amount` | DECIMAL 18,2 | `DECIMAL(18,2)` | `行金额` | 是 | `number` | `{"type":"number","min":0,"message":"金额>=0","trigger":"blur"}` | `50` |

### 表 `wh_stock`

| fieldCode | fieldName | 类型与参数 | fieldType | label | 必填 | formComponent | validateRule | sort |
|-----------|-----------|------------|-----------|-------|------|---------------|--------------|------|
| `WS_F_ID` | `id` | BIGINT | `BIGINT` | `主键` | 是 | `primary_key` | `{}` | `0` |
| `WS_F_WID` | `warehouse_id` | BIGINT | `BIGINT` | `仓库ID` | 是 | `number` | `{}` | `10` |
| `WS_F_PID` | `product_id` | BIGINT | `BIGINT` | `商品ID` | 是 | `number` | `{}` | `20` |
| `WS_F_QOH` | `qty_on_hand` | DECIMAL 18,4 | `DECIMAL(18,4)` | `在库数量` | 是 | `number` | `{}` | `30` |
| `WS_F_QLK` | `qty_locked` | DECIMAL 18,4 | `DECIMAL(18,4)` | `锁定数量` | 是 | `number` | `{}` | `40` |
| `WS_F_UT` | `update_time` | DATETIME | `DATETIME` | `更新时间` | 否 | `datepicker` | `{}` | `50` |

### 表 `sys_dict`

| fieldCode | fieldName | 类型与参数 | fieldType | label | 必填 | formComponent | validateRule | sort |
|-----------|-----------|------------|-----------|-------|------|---------------|--------------|------|
| `SD_F_ID` | `id` | BIGINT | `BIGINT` | `主键` | 是 | `primary_key` | `{}` | `0` |
| `SD_F_TYPE` | `dict_type` | VARCHAR 64 | `VARCHAR(64)` | `字典类型` | 是 | `input` | `{}` | `10` |
| `SD_F_CODE` | `dict_code` | VARCHAR 64 | `VARCHAR(64)` | `字典编码` | 是 | `input` | `{}` | `20` |
| `SD_F_LABEL` | `dict_label` | VARCHAR 128 | `VARCHAR(128)` | `字典标签` | 是 | `input` | `{}` | `30` |
| `SD_F_SORT` | `sort_no` | INT | `INT` | `排序号` | 是 | `number` | `{}` | `40` |
| `SD_F_EN` | `enabled` | TINYINT | `TINYINT` | `是否启用` | 是 | `number` | `{}` | `50` |

### 表 `sys_audit_log`

| fieldCode | fieldName | 类型与参数 | fieldType | label | 必填 | formComponent | validateRule | sort |
|-----------|-----------|------------|-----------|-------|------|---------------|--------------|------|
| `SAL_F_ID` | `id` | BIGINT | `BIGINT` | `主键` | 是 | `primary_key` | `{}` | `0` |
| `SAL_F_TAB` | `biz_table` | VARCHAR 64 | `VARCHAR(64)` | `业务表名` | 是 | `input` | `{}` | `10` |
| `SAL_F_BIZID` | `biz_id` | BIGINT | `BIGINT` | `业务主键` | 是 | `number` | `{}` | `20` |
| `SAL_F_ACT` | `action_code` | VARCHAR 32 | `VARCHAR(32)` | `动作编码` | 是 | `input` | `{}` | `30` |
| `SAL_F_OP` | `operator_name` | VARCHAR 64 | `VARCHAR(64)` | `操作人` | 是 | `input` | `{}` | `40` |
| `SAL_F_CT` | `create_time` | DATETIME | `DATETIME` | `创建时间` | 否 | `datepicker` | `{}` | `50` |

### 字段页其它操作

- 非主键一行：禁用 → 启用  
- 勾选两行：批量禁用 → 批量启用  
- 「查看约束」打开后关闭  
- 分页：`20` → `10`  

---

## 10. 表关联管理

**创建外键约束**：仅关联 1 勾选；关联 2～4 不勾选。

### 关联 1

| 字段 | 值 |
|------|-----|
| 业务系统 | `DEMO_ERP` |
| 关联编码 | `REL_SO_LINE_001` |
| 关联名称 | `订单头与订单明细一对多` |
| 主表 | `sales_order` |
| 主表关联字段 | `SO_F_ID` |
| 从表 | `sales_order_line` |
| 从表外键字段 | `SOL_F_OID` |
| 关联类型 | `ONE_TO_MANY` |
| 关联描述 | `明细.order_id 指向订单头.id` |
| 创建外键约束 | 勾选 |

### 关联 2

| 字段 | 值 |
|------|-----|
| 业务系统 | `DEMO_ERP` |
| 关联编码 | `REL_SO_CUSTOMER_001` |
| 关联名称 | `客户与订单一对多` |
| 主表 | `mdm_customer` |
| 主表关联字段 | `MC_F_ID` |
| 从表 | `sales_order` |
| 从表外键字段 | `SO_F_CID` |
| 关联类型 | `ONE_TO_MANY` |
| 关联描述 | `订单.customer_id 指向客户.id` |
| 创建外键约束 | 不勾选 |

### 关联 3

| 字段 | 值 |
|------|-----|
| 业务系统 | `DEMO_ERP` |
| 关联编码 | `REL_WS_WH_001` |
| 关联名称 | `仓库与库存一对多` |
| 主表 | `mdm_warehouse` |
| 主表关联字段 | `MW_F_ID` |
| 从表 | `wh_stock` |
| 从表外键字段 | `WS_F_WID` |
| 关联类型 | `ONE_TO_MANY` |
| 关联描述 | `库存.warehouse_id 指向仓库.id` |
| 创建外键约束 | 不勾选 |

### 关联 4

| 字段 | 值 |
|------|-----|
| 业务系统 | `DEMO_ERP` |
| 关联编码 | `REL_WS_PD_001` |
| 关联名称 | `商品与库存一对多` |
| 主表 | `mdm_product` |
| 主表关联字段 | `MP_F_ID` |
| 从表 | `wh_stock` |
| 从表外键字段 | `WS_F_PID` |
| 关联类型 | `ONE_TO_MANY` |
| 关联描述 | `库存.product_id 指向商品.id` |
| 创建外键约束 | 不勾选 |

**其它**：点「同步外键」；任选一条编辑后确定；分页切换。

---

## 11. 功能节点管理

顶部：业务系统 `DEMO_ERP`；模块在 `MDM` / `SALES` / `WH` / `CFG` 间切换，每模块 **2** 个节点。

### 模块 MDM

**节点 A**

| 字段 | 值 |
|------|-----|
| 节点编码 | `MDM_NODE_CUST_LIST` |
| 节点名称 | `客户列表` |
| 节点类型 | `LIST_PAGE` |
| 业务系统 | `DEMO_ERP` |
| 关联表 | `mdm_customer` |
| 跳转关系 | `MDM_NODE_CUST_LIST→MDM_NODE_CUST_FORM` |
| 路由路径 | `/customer/list` |
| 组件路径 | `CustomerList.vue` |
| 是否在菜单显示 | 是（1） |
| 节点图标 | `User` |
| 排序号 | `10` |
| 是否启用 | 启用（1） |

**节点 B**

| 字段 | 值 |
|------|-----|
| 节点编码 | `MDM_NODE_CUST_FORM` |
| 节点名称 | `客户表单` |
| 节点类型 | `FORM_PAGE` |
| 业务系统 | `DEMO_ERP` |
| 关联表 | `mdm_customer` |
| 跳转关系 | `MDM_NODE_CUST_FORM→MDM_NODE_CUST_LIST` |
| 路由路径 | `/customer/form` |
| 组件路径 | `CustomerForm.vue` |
| 是否在菜单显示 | 1 |
| 节点图标 | `Edit` |
| 排序号 | `20` |
| 是否启用 | 1 |

### 模块 SALES

**节点 C**

| 字段 | 值 |
|------|-----|
| 节点编码 | `SALES_NODE_ORDER_LIST` |
| 节点名称 | `订单列表` |
| 节点类型 | `LIST_PAGE` |
| 业务系统 | `DEMO_ERP` |
| 关联表 | `sales_order` |
| 跳转关系 | `SALES_NODE_ORDER_LIST→SALES_NODE_ORDER_FORM` |
| 路由路径 | `/order/list` |
| 组件路径 | `OrderList.vue` |
| 是否在菜单显示 | 是（1） |
| 节点图标 | `Tickets` |
| 排序号 | `10` |
| 是否启用 | 启用（1） |

**节点 D**

| 字段 | 值 |
|------|-----|
| 节点编码 | `SALES_NODE_ORDER_FORM` |
| 节点名称 | `订单表单` |
| 节点类型 | `FORM_PAGE` |
| 业务系统 | `DEMO_ERP` |
| 关联表 | `sales_order` |
| 跳转关系 | `SALES_NODE_ORDER_FORM→SALES_NODE_LINE_LIST` |
| 路由路径 | `/order/form` |
| 组件路径 | `OrderForm.vue` |
| 是否在菜单显示 | 是（1） |
| 节点图标 | `Document` |
| 排序号 | `20` |
| 是否启用 | 启用（1） |

### 模块 WH

**节点 E**

| 字段 | 值 |
|------|-----|
| 节点编码 | `WH_NODE_STOCK_LIST` |
| 节点名称 | `库存列表` |
| 节点类型 | `LIST_PAGE` |
| 业务系统 | `DEMO_ERP` |
| 关联表 | `wh_stock` |
| 跳转关系 | `WH_NODE_STOCK_LIST→WH_NODE_STOCK_DETAIL` |
| 路由路径 | `/stock/list` |
| 组件路径 | `StockList.vue` |
| 是否在菜单显示 | 是（1） |
| 节点图标 | `Box` |
| 排序号 | `10` |
| 是否启用 | 启用（1） |

**节点 F**

| 字段 | 值 |
|------|-----|
| 节点编码 | `WH_NODE_STOCK_DETAIL` |
| 节点名称 | `库存详情` |
| 节点类型 | `DETAIL_PAGE` |
| 业务系统 | `DEMO_ERP` |
| 关联表 | `wh_stock` |
| 跳转关系 | `WH_NODE_STOCK_DETAIL→WH_NODE_STOCK_LIST` |
| 路由路径 | `/stock/detail` |
| 组件路径 | `StockDetail.vue` |
| 是否在菜单显示 | **否（0）** |
| 节点图标 | `View` |
| 排序号 | `20` |
| 是否启用 | 启用（1） |

### 模块 CFG

**节点 G**

| 字段 | 值 |
|------|-----|
| 节点编码 | `CFG_NODE_DICT_LIST` |
| 节点名称 | `字典列表` |
| 节点类型 | `LIST_PAGE` |
| 业务系统 | `DEMO_ERP` |
| 关联表 | `sys_dict` |
| 跳转关系 | `CFG_NODE_DICT_LIST→CFG_NODE_DICT_IMPORT` |
| 路由路径 | `/dict/list` |
| 组件路径 | `DictList.vue` |
| 是否在菜单显示 | 是（1） |
| 节点图标 | `Collection` |
| 排序号 | `10` |
| 是否启用 | 启用（1） |

**节点 H**

| 字段 | 值 |
|------|-----|
| 节点编码 | `CFG_NODE_DICT_IMPORT` |
| 节点名称 | `字典批量导入` |
| 节点类型 | `BATCH_IMPORT_PAGE` |
| 业务系统 | `DEMO_ERP` |
| 关联表 | `sys_dict` |
| 跳转关系 | `CFG_NODE_DICT_IMPORT→CFG_NODE_DICT_LIST` |
| 路由路径 | `/dict/import` |
| 组件路径 | `DictImport.vue` |
| 是否在菜单显示 | 是（1） |
| 节点图标 | `Upload` |
| 排序号 | `20` |
| 是否启用 | 启用（1） |

---

## 12. 业务规则管理

业务系统选 `演示ERP业务系统`；按模块切换新增 **6** 条（六种 `ruleType` 各一条）。

### 规则 1 — 模块 MDM

| 字段 | 值 |
|------|-----|
| 规则编码 | `RULE_MDM_VAL_001` |
| 规则类型 | `VALIDATION_RULE` |
| 规则内容 | `{"field":"customer_code","type":"unique","message":"客户编码已存在"}` |
| 描述 | `客户编码唯一校验` |

### 规则 2 — 模块 MDM

| 规则编码 | `RULE_MDM_SEARCH_001` |
| 规则类型 | `SEARCH_RULE` |
| 规则内容 | `{"defaultFields":["customer_code","customer_name","status"],"allowFuzzy":["customer_name"],"maxConditions":8}` |
| 描述 | `客户列表默认检索字段` |

### 规则 3 — 模块 SALES

| 规则编码 | `RULE_SALES_DISP_001` |
| 规则类型 | `DISPLAY_RULE` |
| 规则内容 | `{"listColumns":["order_no","customer_id","order_status","create_time"],"formSections":[{"title":"基本信息","fields":["order_no","customer_id"]},{"title":"状态","fields":["order_status"]}]}` |
| 描述 | `订单列表与表单分区显示` |

### 规则 4 — 模块 SALES

| 规则编码 | `RULE_SALES_PROC_001` |
| 规则类型 | `PROCESS_RULE` |
| 规则内容 | `{"states":["DRAFT","CONFIRMED","CANCELLED"],"transitions":[{"from":"DRAFT","to":"CONFIRMED","label":"确认"},{"from":"DRAFT","to":"CANCELLED","label":"作废"},{"from":"CONFIRMED","to":"CANCELLED","label":"关闭"}]}` |
| 描述 | `订单状态流转定义` |

### 规则 5 — 模块 WH

| 规则编码 | `RULE_WH_RPT_001` |
| 规则类型 | `REPORT_RULE` |
| 规则内容 | `{"datasetTable":"wh_stock","dimensions":["warehouse_id","product_id"],"metrics":[{"name":"sum_on_hand","expr":"SUM(qty_on_hand)"},{"name":"sum_locked","expr":"SUM(qty_locked)"}],"filters":[{"field":"qty_on_hand","op":">","value":0}]}` |
| 描述 | `库存结存汇总报表定义` |

### 规则 6 — 模块 CFG

| 规则编码 | `RULE_CFG_BATCH_001` |
| 规则类型 | `BATCH_RULE` |
| 规则内容 | `{"maxBatchSize":500,"idempotent":true,"onError":"STOP","allowedOps":["INSERT","UPDATE"]}` |
| 描述 | `字典批量导入上限与错误策略` |

**其它**：每条点「预览」；编辑一条保存；分页 `20` → `10`。

---

## 13. SQL 执行

依次在编辑器中执行（或拆分执行）。**注意**：若表已存在，`CREATE TABLE` 可能失败；系统禁止 `DROP`，请勿用删表重试。

### 块 1 — 探测

```sql
SELECT 1 AS probe_connection, 'DEMO_ERP' AS business_code, DATABASE() AS current_db;
```

### 块 2 — 八张建表

```sql
CREATE TABLE IF NOT EXISTS mdm_customer (
  id BIGINT NOT NULL AUTO_INCREMENT,
  customer_code VARCHAR(32) NOT NULL,
  customer_name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mdm_customer_code (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mdm_product (
  id BIGINT NOT NULL AUTO_INCREMENT,
  product_code VARCHAR(64) NOT NULL,
  product_name VARCHAR(256) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mdm_product_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mdm_warehouse (
  id BIGINT NOT NULL AUTO_INCREMENT,
  warehouse_code VARCHAR(32) NOT NULL,
  warehouse_name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mdm_warehouse_code (warehouse_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  customer_id BIGINT NOT NULL,
  order_status ENUM('DRAFT','CONFIRMED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sales_order_no (order_no),
  KEY idx_sales_order_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_order_line (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  qty DECIMAL(14,4) NOT NULL,
  line_amount DECIMAL(18,2) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_sales_order_line_order (order_id),
  KEY idx_sales_order_line_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wh_stock (
  id BIGINT NOT NULL AUTO_INCREMENT,
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  qty_on_hand DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
  qty_locked DECIMAL(18,4) NOT NULL DEFAULT 0.0000,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wh_stock_wh_prod (warehouse_id, product_id),
  KEY idx_wh_stock_wh (warehouse_id),
  KEY idx_wh_stock_pd (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_dict (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dict_type VARCHAR(64) NOT NULL,
  dict_code VARCHAR(64) NOT NULL,
  dict_label VARCHAR(128) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_dict_type_code (dict_type, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  biz_table VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  action_code VARCHAR(32) NOT NULL,
  operator_name VARCHAR(64) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sys_audit_biz (biz_table, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 块 3 — 演示数据

```sql
INSERT INTO mdm_customer (customer_code, customer_name, status, remark) VALUES ('CUST0001','华东贸易有限公司',1,'模拟客户');
INSERT INTO mdm_product (product_code, product_name, status, remark) VALUES ('SKU0001','标准螺栓M8',1,'模拟商品');
INSERT INTO mdm_warehouse (warehouse_code, warehouse_name, status, remark) VALUES ('WH01','一号成品仓',1,'模拟仓库');
INSERT INTO sales_order (order_no, customer_id, order_status) VALUES ('SO2026000001',1,'DRAFT');
INSERT INTO sales_order_line (order_id, line_no, product_id, qty, line_amount) VALUES (1,1,1,10.0000,199.99);
INSERT INTO wh_stock (warehouse_id, product_id, qty_on_hand, qty_locked) VALUES (1,1,100.0000,0.0000);
INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort_no, enabled) VALUES ('ORDER_STATUS','DRAFT','草稿',10,1);
INSERT INTO sys_audit_log (biz_table, biz_id, action_code, operator_name) VALUES ('sales_order',1,'CREATE','admin');
```

### 块 4 — UPDATE

```sql
UPDATE sales_order SET order_status = 'CONFIRMED' WHERE order_no = 'SO2026000001';
```

### 块 5 — 故意错误（失败路径）

```sql
SELECT * FROM table_that_does_not_exist_for_error_test_xyz;
```

### 界面按钮覆盖

- 「格式化」「清空」  
- 「导入SQL」→ 本地文件 / 网络 URL 各试一次  
- 再执行块 1 验证正常  

---

## 14. 代码生成

1. 业务系统：`演示ERP业务系统`（`DEMO_ERP`）  
2. **选择表**：先清空  
3. 包名只读：`com.demo.erp`  
4. 「接口 + 实现类」先 **关**（传统 Service）  
5. 点「生成代码」  

### 折叠「Java相关」— 依次打开标签

| 顺序 | 标签名 |
|------|--------|
| 1 | SQL建表语句 |
| 2 | Entity实体类 |
| 3 | Controller |
| 4 | Service |
| 5 | Mapper接口 |
| 6 | Mapper.xml |
| 7 | 启动类 |
| 8 | CORS配置 |

每个标签内可点「刷新」「复制」。

### 折叠「前端相关」

| 顺序 | 标签名 | 备注 |
|------|--------|------|
| 1 | Vue列表页 | 可「预览」 |
| 2 | Vue表单页 | 可「预览」 |
| 3 | 登录页 | 可「下载」「预览」 |
| 4 | Routes | 可「生成整合路由」 |
| 5 | API请求文件 | 可「下载」 |
| 6 | 请求工具类 | 可「下载」 |
| 7 | 认证API文件 | 可「下载」 |
| 8 | .env配置 | 可「下载」 |

### 折叠「工具相关」

| 顺序 | 标签名 |
|------|--------|
| 1 | 配置文件 |
| 2 | MyBatis配置 |
| 3 | pom.xml |
| 4 | Result类 |
| 5 | PageRequest类 |
| 6 | PageResult类 |

### 第二轮

6. **选择表**：`sales_order`（销售订单头表）  
7. 「接口 + 实现类」拨到 **开**  
8. 再点「生成代码」  
9. 「Java相关」中打开：`Service接口`、`Service实现类`  
10. 点「测试代码」  
11. 点「部署指南」打开后关闭  

---

## 15. 操作日志

| 次数 | 所属模块 | 操作类型 | 开始时间 | 结束时间 |
|------|----------|----------|----------|----------|
| 1 | 表管理 | 新增 | 当天 00:00:00 | 当天 23:59:59 |
| 2 | 代码生成 | 生成SQL | — | — |
| 3 | SQL执行 | 执行SQL | — | — |
| 4 | 业务系统管理 | 关联 | — | — |

每次点「查询」；第一次后点「重置」。分页：`10` → `20` → `50` → `10`。

---

## 16. 侧栏与标签页

- 侧栏折叠按钮：收起 → 展开  
- 顶部标签：打开「库管理」「代码生成」切换；右键菜单（若有）试关闭其它/全部  

---

## 自检清单

| 项 | 数量/要求 |
|----|-----------|
| 物理库登记 | 1 |
| 模块类型 | 3 |
| 业务系统 | 1 + 关联 4 模块 |
| 模块 | 4 |
| 数据表 | 8 |
| 每表字段 | 6（共 48 条） |
| 表关联 | 4 |
| 功能节点 | 8 |
| 业务规则（类型全覆盖） | 6 |
| 代码生成 | 全业务一次 + 单表一次 + 两种 Service 模式 |
| SQL | SELECT + CREATE + INSERT + UPDATE + 错误语句 + 导入/格式化 |

---

*文档路径：`docs/full-system-simulation-guide.md`*
