# 通用元数据管理系统API测试文档

## 1. 项目信息

- **项目名称**：通用元数据管理系统
- **API版本**：1.0.0
- **基本URL**：http://localhost:8080/metadata-system/api
- **认证方式**：Session认证

## 2. API分类

### 2.1 认证管理

#### 2.1.1 用户登录

**API路径**：/auth/login
**请求方法**：POST
**功能描述**：用户登录认证，返回用户信息

**请求示例**：
```json
{
  "username": "admin",
  "password": "123456"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "username": "admin"
  }
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "用户名或密码错误",
  "data": null
}
```

#### 2.1.2 用户退出

**API路径**：/auth/logout
**请求方法**：POST
**功能描述**：用户退出登录，清空会话信息

**请求示例**：
```
POST /api/auth/logout
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

#### 2.1.3 修改密码

**API路径**：/auth/changePassword
**请求方法**：POST
**功能描述**：用户修改密码，需要验证原密码

**请求示例**：
```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "原密码错误",
  "data": null
}
```

### 2.2 业务系统管理

#### 2.2.1 新增业务系统

**API路径**：/businessSystem/add
**请求方法**：POST
**功能描述**：添加新的业务系统

**请求示例**：
```json
{
  "businessCode": "SYS001",
  "businessName": "学生管理系统",
  "description": "学生信息管理系统"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "业务系统编码已存在",
  "data": null
}
```

#### 2.2.2 更新业务系统

**API路径**：/businessSystem/update
**请求方法**：POST
**功能描述**：更新业务系统信息

**请求示例**：
```json
{
  "id": 1,
  "businessCode": "SYS001",
  "businessName": "学生管理系统V2",
  "description": "学生信息管理系统V2"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "业务系统不存在",
  "data": null
}
```

#### 2.2.3 删除业务系统

**API路径**：/businessSystem/delete/{id}
**请求方法**：DELETE
**功能描述**：根据ID删除业务系统

**请求示例**：
```
DELETE /api/businessSystem/delete/1
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "业务系统不存在",
  "data": null
}
```

#### 2.2.4 查询业务系统详情

**API路径**：/businessSystem/{businessCode}
**请求方法**：GET
**功能描述**：根据业务系统编码查询详情

**请求示例**：
```
GET /api/businessSystem/SYS001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "businessCode": "SYS001",
    "businessName": "学生管理系统",
    "description": "学生信息管理系统",
    "createTime": "2023-01-01 10:00:00",
    "updateTime": "2023-01-01 10:00:00"
  }
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "业务系统不存在",
  "data": null
}
```

#### 2.2.5 查询业务系统列表

**API路径**：/businessSystem/list
**请求方法**：GET
**功能描述**：查询所有业务系统

**请求示例**：
```
GET /api/businessSystem/list
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "businessCode": "SYS001",
      "businessName": "学生管理系统",
      "description": "学生信息管理系统"
    },
    {
      "id": 2,
      "businessCode": "SYS002",
      "businessName": "课程管理系统",
      "description": "课程信息管理系统"
    }
  ]
}
```

#### 2.2.6 获取默认业务系统

**API路径**：/businessSystem/default
**请求方法**：GET
**功能描述**：获取默认的业务系统

**请求示例**：
```
GET /api/businessSystem/default
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "businessCode": "SYS001",
    "businessName": "学生管理系统",
    "description": "学生信息管理系统"
  }
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "未设置默认业务系统",
  "data": null
}
```

#### 2.2.7 关联模块到业务系统

**API路径**：/businessSystem/associateModules
**请求方法**：POST
**功能描述**：将多个模块关联到指定业务系统

**请求示例**：
```json
{
  "businessCode": "SYS001",
  "moduleCodes": ["MOD001", "MOD002"]
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "业务系统不存在",
  "data": null
}
```

#### 2.2.8 解除模块关联

**API路径**：/businessSystem/disassociateModules
**请求方法**：POST
**功能描述**：解除多个模块与指定业务系统的关联

**请求示例**：
```json
{
  "businessCode": "SYS001",
  "moduleCodes": ["MOD001"]
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "业务系统不存在",
  "data": null
}
```

#### 2.2.9 获取已关联模块

**API路径**：/businessSystem/associatedModules/{businessCode}
**请求方法**：GET
**功能描述**：获取指定业务系统已关联的模块列表

**请求示例**：
```
GET /api/businessSystem/associatedModules/SYS001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": ["MOD001", "MOD002"]
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "业务系统不存在",
  "data": null
}
```

### 2.3 表管理

#### 2.3.1 新增表

**API路径**：/table/add
**请求方法**：POST
**功能描述**：添加新的元数据表

**请求示例**：
```json
{
  "tableCode": "STU001",
  "tableName": "学生表",
  "businessCode": "SYS001",
  "moduleCode": "MOD001"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表编码已存在",
  "data": null
}
```

#### 2.3.2 更新表

**API路径**：/table/update
**请求方法**：POST
**功能描述**：更新元数据表信息

**请求示例**：
```json
{
  "id": 1,
  "tableCode": "STU001",
  "tableName": "学生信息表",
  "businessCode": "SYS001",
  "moduleCode": "MOD001"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.3.3 删除表

**API路径**：/table/delete/{id}
**请求方法**：DELETE
**功能描述**：根据ID删除元数据表

**请求示例**：
```
DELETE /api/table/delete/1
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.3.4 批量删除表

**API路径**：/table/batchDelete
**请求方法**：POST
**功能描述**：根据ID列表批量删除元数据表

**请求示例**：
```json
{
  "ids": [1, 2, 3]
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "批量删除失败",
  "data": null
}
```

#### 2.3.5 查询表详情

**API路径**：/table/{tableCode}
**请求方法**：GET
**功能描述**：根据表编码查询表详情

**请求示例**：
```
GET /api/table/STU001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "tableCode": "STU001",
    "tableName": "学生表",
    "businessCode": "SYS001",
    "moduleCode": "MOD001",
    "createTime": "2023-01-01 10:00:00",
    "updateTime": "2023-01-01 10:00:00"
  }
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.3.6 查询表列表

**API路径**：/table/list
**请求方法**：GET
**功能描述**：查询表列表，支持分页和条件查询

**请求示例**：
```
GET /api/table/list?tableName=学生&businessCode=SYS001&current=1&size=10
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "tableCode": "STU001",
        "tableName": "学生表",
        "businessCode": "SYS001",
        "moduleCode": "MOD001"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

#### 2.3.7 根据模块查询表

**API路径**：/table/listByModule/{moduleCode}
**请求方法**：GET
**功能描述**：根据模块编码查询关联的表列表

**请求示例**：
```
GET /api/table/listByModule/MOD001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "tableCode": "STU001",
      "tableName": "学生表",
      "businessCode": "SYS001",
      "moduleCode": "MOD001"
    }
  ]
}
```

### 2.4 字段管理

#### 2.4.1 新增字段

**API路径**：/field/add
**请求方法**：POST
**功能描述**：添加新的元数据字段

**请求示例**：
```json
{
  "fieldCode": "STU_NAME",
  "fieldName": "学生姓名",
  "tableCode": "STU001",
  "dataType": "VARCHAR",
  "length": 50
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "字段编码已存在",
  "data": null
}
```

#### 2.4.2 更新字段

**API路径**：/field/update
**请求方法**：POST
**功能描述**：更新元数据字段信息

**请求示例**：
```json
{
  "id": 1,
  "fieldCode": "STU_NAME",
  "fieldName": "姓名",
  "tableCode": "STU001",
  "dataType": "VARCHAR",
  "length": 50
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "字段不存在",
  "data": null
}
```

#### 2.4.3 删除字段

**API路径**：/field/delete/{id}
**请求方法**：DELETE
**功能描述**：根据ID删除元数据字段

**请求示例**：
```
DELETE /api/field/delete/1
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "字段不存在",
  "data": null
}
```

#### 2.4.4 批量删除字段

**API路径**：/field/batchDelete
**请求方法**：POST
**功能描述**：根据ID列表批量删除元数据字段

**请求示例**：
```json
{
  "ids": [1, 2, 3]
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "批量删除失败",
  "data": null
}
```

#### 2.4.5 查询字段列表

**API路径**：/field/list/{tableCode}
**请求方法**：GET
**功能描述**：根据表编码查询字段列表，支持分页

**请求示例**：
```
GET /api/field/list/STU001?current=1&size=10
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "fieldCode": "STU_NAME",
        "fieldName": "学生姓名",
        "tableCode": "STU001",
        "dataType": "VARCHAR",
        "length": 50
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

#### 2.4.6 获取约束列表

**API路径**：/field/constraint/list/{tableCode}
**请求方法**：GET
**功能描述**：根据表编码查询约束列表

**请求示例**：
```
GET /api/field/constraint/list/STU001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "constraintName": "PK_STU001",
      "constraintType": "PRIMARY KEY",
      "fieldNames": ["id"]
    },
    {
      "constraintName": "UK_STU_CODE",
      "constraintType": "UNIQUE KEY",
      "fieldNames": ["stu_code"]
    }
  ]
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.4.7 删除约束

**API路径**：/field/constraint/delete
**请求方法**：POST
**功能描述**：删除表的约束

**请求示例**：
```json
{
  "id": 1,
  "constraintName": "PK_STU001",
  "tableName": "student",
  "tableCode": "STU001",
  "fieldName": "id"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "约束不存在",
  "data": null
}
```

### 2.5 表关联关系管理

#### 2.5.1 新增表关联关系

**API路径**：/relation/add
**请求方法**：POST
**功能描述**：新增表关联关系

**请求示例**：
```json
{
  "mainTableCode": "STU001",
  "mainFieldCode": "CLASS_ID",
  "slaveTableCode": "CLASS001",
  "slaveFieldCode": "ID",
  "relationType": "ONE_TO_MANY"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表关联关系已存在",
  "data": null
}
```

#### 2.5.2 更新表关联关系

**API路径**：/relation/update
**请求方法**：POST
**功能描述**：更新表关联关系

**请求示例**：
```json
{
  "id": 1,
  "mainTableCode": "STU001",
  "mainFieldCode": "CLASS_ID",
  "slaveTableCode": "CLASS001",
  "slaveFieldCode": "ID",
  "relationType": "ONE_TO_MANY"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表关联关系不存在",
  "data": null
}
```

#### 2.5.3 删除表关联关系

**API路径**：/relation/delete/{id}
**请求方法**：DELETE
**功能描述**：删除表关联关系

**请求示例**：
```
DELETE /api/relation/delete/1
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表关联关系不存在",
  "data": null
}
```

#### 2.5.4 根据主表查询关联关系

**API路径**：/relation/listByMain/{mainTableCode}
**请求方法**：GET
**功能描述**：根据主表查询关联关系

**请求示例**：
```
GET /api/relation/listByMain/STU001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "mainTableCode": "STU001",
      "mainFieldCode": "CLASS_ID",
      "slaveTableCode": "CLASS001",
      "slaveFieldCode": "ID",
      "relationType": "ONE_TO_MANY"
    }
  ]
}
```

#### 2.5.5 根据从表查询关联关系

**API路径**：/relation/listBySlave/{slaveTableCode}
**请求方法**：GET
**功能描述**：根据从表查询关联关系

**请求示例**：
```
GET /api/relation/listBySlave/CLASS001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "mainTableCode": "STU001",
      "mainFieldCode": "CLASS_ID",
      "slaveTableCode": "CLASS001",
      "slaveFieldCode": "ID",
      "relationType": "ONE_TO_MANY"
    }
  ]
}
```

#### 2.5.6 查询表关联关系列表

**API路径**：/relation/list
**请求方法**：GET
**功能描述**：查询表关联关系列表

**请求示例**：
```
GET /api/relation/list?businessCode=SYS001&current=1&size=10
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "mainTableCode": "STU001",
        "mainFieldCode": "CLASS_ID",
        "slaveTableCode": "CLASS001",
        "slaveFieldCode": "ID",
        "relationType": "ONE_TO_MANY"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

#### 2.5.7 创建外键约束

**API路径**：/relation/createForeignKey
**请求方法**：POST
**功能描述**：创建外键约束

**请求示例**：
```json
{
  "mainTableCode": "STU001",
  "mainFieldCode": "CLASS_ID",
  "slaveTableCode": "CLASS001",
  "slaveFieldCode": "ID"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "外键创建成功"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "外键创建失败，原因：字段类型不匹配",
  "data": null
}
```

#### 2.5.8 同步外键约束

**API路径**：/relation/syncForeignKeys
**请求方法**：POST
**功能描述**：同步外键约束

**请求示例**：
```
POST /api/relation/syncForeignKeys?tableCode=STU001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "外键同步成功"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "外键同步失败，原因：表不存在",
  "data": null
}
```

### 2.6 业务规则管理

#### 2.6.1 添加业务规则

**API路径**：/rule/add
**请求方法**：POST
**功能描述**：添加业务规则

**请求示例**：
```json
{
  "ruleName": "学生年龄规则",
  "ruleContent": "年龄必须在6-25岁之间",
  "moduleCode": "MOD001"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "业务规则名称已存在",
  "data": null
}
```

#### 2.6.2 更新业务规则

**API路径**：/rule/update
**请求方法**：POST
**功能描述**：更新业务规则

**请求示例**：
```json
{
  "id": 1,
  "ruleName": "学生年龄规则",
  "ruleContent": "年龄必须在6-30岁之间",
  "moduleCode": "MOD001"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "业务规则不存在",
  "data": null
}
```

#### 2.6.3 删除业务规则

**API路径**：/rule/delete/{id}
**请求方法**：DELETE
**功能描述**：根据id删除业务规则

**请求示例**：
```
DELETE /api/rule/delete/1
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "业务规则不存在",
  "data": null
}
```

#### 2.6.4 根据模块查询业务规则

**API路径**：/rule/list/{moduleCode}
**请求方法**：GET
**功能描述**：通过模块唯一代码分页查询

**请求示例**：
```
GET /api/rule/list/MOD001?current=1&size=10
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "ruleName": "学生年龄规则",
        "ruleContent": "年龄必须在6-25岁之间",
        "moduleCode": "MOD001",
        "createTime": "2023-01-01 10:00:00",
        "updateTime": "2023-01-01 10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

### 2.7 代码生成管理

#### 2.7.1 生成SQL语句

**API路径**：/codegen/sql/{tableCode}
**请求方法**：GET
**功能描述**：生成建表SQL

**请求示例**：
```
GET /api/codegen/sql/STU001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "CREATE TABLE student (\n  id BIGINT PRIMARY KEY AUTO_INCREMENT,\n  stu_code VARCHAR(20) UNIQUE NOT NULL,\n  stu_name VARCHAR(50) NOT NULL,\n  gender VARCHAR(10),\n  age INT,\n  class_id BIGINT,\n  create_time DATETIME,\n  update_time DATETIME\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.7.2 生成实体类

**API路径**：/codegen/entity/{tableCode}
**请求方法**：GET
**功能描述**：生成实体类

**请求示例**：
```
GET /api/codegen/entity/STU001?packageName=com.example.entity
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "package com.example.entity;\n\nimport java.io.Serializable;\nimport java.util.Date;\n\n/**\n * 学生表实体类\n */\npublic class Student implements Serializable {\n    private static final long serialVersionUID = 1L;\n\n    private Long id;\n    private String stuCode;\n    private String stuName;\n    private String gender;\n    private Integer age;\n    private Long classId;\n    private Date createTime;\n    private Date updateTime;\n    \n    // getter和setter方法\n    // ...\n}\n"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.7.3 生成Controller类

**API路径**：/codegen/controller/{tableCode}
**请求方法**：GET
**功能描述**：生成Controller类

**请求示例**：
```
GET /api/codegen/controller/STU001?packageName=com.example.controller
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "package com.example.controller;\n\nimport com.example.common.Result;\nimport com.example.entity.Student;\nimport com.example.service.StudentService;\nimport io.swagger.v3.oas.annotations.Operation;\nimport io.swagger.v3.oas.annotations.tags.Tag;\nimport org.springframework.beans.factory.annotation.Autowired;\nimport org.springframework.web.bind.annotation.*;\n\n@RestController\n@RequestMapping(\"/api/student\")\n@Tag(name = \"学生管理\", description = \"学生相关API\")\npublic class StudentController {\n    \n    @Autowired\n    private StudentService studentService;\n    \n    // 增删改查方法\n    // ...\n}\n"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.7.4 生成Service类

**API路径**：/codegen/service/{tableCode}
**请求方法**：GET
**功能描述**：生成Service类

**请求示例**：
```
GET /api/codegen/service/STU001?packageName=com.example.service
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "package com.example.service;\n\nimport com.example.entity.Student;\nimport com.example.common.PageResult;\nimport com.example.common.PageRequest;\n\nimport java.util.List;\n\npublic interface StudentService {\n    \n    void add(Student student);\n    \n    void update(Student student);\n    \n    void delete(Long id);\n    \n    Student getById(Long id);\n    \n    List<Student> list();\n    \n    PageResult<Student> page(PageRequest pageRequest);\n    \n}\n"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.7.5 生成Mapper接口

**API路径**：/codegen/mapper/{tableCode}
**请求方法**：GET
**功能描述**：生成Mapper接口

**请求示例**：
```
GET /api/codegen/mapper/STU001?packageName=com.example.mapper
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "package com.example.mapper;\n\nimport com.example.entity.Student;\nimport org.apache.ibatis.annotations.Mapper;\n\nimport java.util.List;\n\n@Mapper\npublic interface StudentMapper {\n    \n    void insert(Student student);\n    \n    void update(Student student);\n    \n    void delete(Long id);\n    \n    Student selectById(Long id);\n    \n    List<Student> selectAll();\n    \n}\n"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.7.6 生成MapperXML

**API路径**：/codegen/mapperxml/{tableCode}
**请求方法**：GET
**功能描述**：生成MapperXML

**请求示例**：
```
GET /api/codegen/mapperxml/STU001?packageName=com.example.mapper
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>
<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">
<mapper namespace=\"com.example.mapper.StudentMapper\">
    
    <resultMap id=\"BaseResultMap\" type=\"com.example.entity.Student\">
        <id column=\"id\" property=\"id\" />
        <result column=\"stu_code\" property=\"stuCode\" />
        <result column=\"stu_name\" property=\"stuName\" />
        <result column=\"gender\" property=\"gender\" />
        <result column=\"age\" property=\"age\" />
        <result column=\"class_id\" property=\"classId\" />
        <result column=\"create_time\" property=\"createTime\" />
        <result column=\"update_time\" property=\"updateTime\" />
    </resultMap>
    
    <insert id=\"insert\" parameterType=\"com.example.entity.Student\">
        INSERT INTO student (stu_code, stu_name, gender, age, class_id, create_time, update_time)
        VALUES (#{stuCode}, #{stuName}, #{gender}, #{age}, #{classId}, #{createTime}, #{updateTime})
    </insert>
    
    <!-- 其他SQL语句 -->
    <!-- ... -->
</mapper>"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.7.7 生成Vue列表页

**API路径**：/codegen/vue/list/{tableCode}
**请求方法**：GET
**功能描述**：生成Vue列表页

**请求示例**：
```
GET /api/codegen/vue/list/STU001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "<template>\n  <div class=\"student-list\">\n    <h1>学生列表</h1>\n    <!-- 搜索区域 -->\n    <div class=\"search-area\">\n      <el-input v-model=\"searchParams.stuName\" placeholder=\"请输入学生姓名\" clearable style=\"width: 200px; margin-right: 10px;\" />
      <el-button type=\"primary\" @click=\"handleSearch\">搜索</el-button>\n      <el-button @click=\"handleReset\">重置</el-button>\n      <el-button type=\"success\" @click=\"handleAdd\">新增</el-button>\n    </div>\n    <!-- 表格区域 -->\n    <el-table :data=\"tableData\" stripe style=\"width: 100%\">\n      <el-table-column prop=\"id\" label=\"ID\" width=\"80\" />
      <el-table-column prop=\"stuCode\" label=\"学号\" width=\"120\" />
      <el-table-column prop=\"stuName\" label=\"姓名\" width=\"120\" />
      <el-table-column prop=\"gender\" label=\"性别\" width=\"80\" />
      <el-table-column prop=\"age\" label=\"年龄\" width=\"80\" />
      <el-table-column prop=\"classId\" label=\"班级ID\" width=\"100\" />
      <el-table-column label=\"操作\" width=\"200\" fixed=\"right\">\n        <template v-slot=\"scope\">\n          <el-button size=\"small\" type=\"primary\" @click=\"handleEdit(scope.row)\">编辑</el-button>\n          <el-button size=\"small\" type=\"danger\" @click=\"handleDelete(scope.row)\">删除</el-button>\n        </template>\n      </el-table-column>\n    </el-table>\n    <!-- 分页区域 -->\n    <div class=\"pagination\">\n      <el-pagination\n        v-model:current-page=\"pagination.current\"\n        v-model:page-size=\"pagination.size\"\n        :page-sizes=\"[10, 20, 50, 100]\"\n        layout=\"total, sizes, prev, pager, next, jumper\"\n        :total=\"pagination.total\"\n        @size-change=\"handleSizeChange\"\n        @current-change=\"handleCurrentChange\"\n      /\n    </div>\n  </div>\n</template>\n\n<script setup>\n// Vue 3 Composition API 代码\n// ...\n</script>\n\n<style scoped>\n/* 样式代码 */\n/* ... */\n</style>"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.7.8 生成Vue表单页

**API路径**：/codegen/vue/form/{tableCode}
**请求方法**：GET
**功能描述**：生成Vue表单页

**请求示例**：
```
GET /api/codegen/vue/form/STU001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "<template>\n  <div class=\"student-form\">\n    <el-dialog v-model=\"dialogVisible\" title=\"学生表单\" width=\"500px\" :close-on-click-modal=\"false\">\n      <el-form :model=\"formData\" :rules=\"rules\" ref=\"formRef\" label-width=\"100px\">\n        <el-form-item label=\"学号\" prop=\"stuCode\">\n          <el-input v-model=\"formData.stuCode\" placeholder=\"请输入学号\" /\n        </el-form-item>\n        <el-form-item label=\"姓名\" prop=\"stuName\">\n          <el-input v-model=\"formData.stuName\" placeholder=\"请输入姓名\" /\n        </el-form-item>\n        <el-form-item label=\"性别\" prop=\"gender\">\n          <el-select v-model=\"formData.gender\" placeholder=\"请选择性别\">\n            <el-option label=\"男\" value=\"男\" /\n            <el-option label=\"女\" value=\"女\" /\n          </el-select>\n        </el-form-item>\n        <el-form-item label=\"年龄\" prop=\"age\">\n          <el-input-number v-model=\"formData.age\" :min=\"0\" :max=\"150\" placeholder=\"请输入年龄\" /\n        </el-form-item>\n        <el-form-item label=\"班级ID\" prop=\"classId\">\n          <el-input v-model=\"formData.classId\" placeholder=\"请输入班级ID\" /\n        </el-form-item>\n      </el-form>\n      <template #footer>\n        <span class=\"dialog-footer\">\n          <el-button @click=\"dialogVisible = false\">取消</el-button>\n          <el-button type=\"primary\" @click=\"handleSubmit\">确定</el-button>\n        </span>\n      </template>\n    </el-dialog>\n  </div>\n</template>\n\n<script setup>\n// Vue 3 Composition API 代码\n// ...\n</script>\n\n<style scoped>\n/* 样式代码 */\n/* ... */\n</style>"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.7.9 生成所有代码

**API路径**：/codegen/all/{tableCode}
**请求方法**：GET
**功能描述**：生成所有代码

**请求示例**：
```
GET /api/codegen/all/STU001?packageName=com.example
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sql": "CREATE TABLE student (...);",
    "entity": "package com.example.entity;\n\npublic class Student implements Serializable {...}",
    "controller": "package com.example.controller;\n\n@RestController\npublic class StudentController {...}",
    "service": "package com.example.service;\n\npublic interface StudentService {...}",
    "mapper": "package com.example.mapper;\n\n@Mapper\npublic interface StudentMapper {...}",
    "mapperXml": "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<!DOCTYPE mapper PUBLIC ...>",
    "vueList": "<template>\n  <div class=\"student-list\">...</div>\n</template>",
    "vueForm": "<template>\n  <div class=\"student-form\">...</div>\n</template>"
  }
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

#### 2.7.10 生成路由配置

**API路径**：/codegen/routes/{tableCode}
**请求方法**：GET
**功能描述**：生成路由配置

**请求示例**：
```
GET /api/codegen/routes/STU001
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "[\n  {\n    path: '/student',\n    name: 'Student',\n    component: () => import('@/views/StudentList.vue'),\n    meta: {\n      title: '学生管理',\n      icon: 'el-icon-user'\n    }\n  },\n  {\n    path: '/student/form/:id?',\n    name: 'StudentForm',\n    component: () => import('@/views/StudentForm.vue'),\n    meta: {\n      title: '学生表单',\n      hidden: true\n    }\n  }\n]"
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "表不存在",
  "data": null
}
```

### 2.8 操作日志管理

#### 2.8.1 查询操作日志列表

**API路径**：/operationLog/list
**请求方法**：GET
**功能描述**：查询操作日志列表

**请求示例**：
```
GET /api/operationLog/list?module=table&operateType=add&startTime=2023-01-01&endTime=2023-12-31&current=1&size=10
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "module": "table",
        "operateType": "add",
        "operateContent": "新增表",
        "operateUser": "admin",
        "operateTime": "2023-01-01 10:00:00",
        "ipAddress": "127.0.0.1"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

**失败响应示例**：
```json
{
  "code": 500,
  "message": "查询失败，原因：数据库连接异常",
  "data": null
}
```

## 3. 通用响应格式

所有API接口返回的响应格式统一为：

```json
{
  "code": 200,           // 响应码，200表示成功，其他表示失败
  "message": "success",  // 响应消息，失败时返回错误信息
  "data": null           // 响应数据，根据API不同返回不同类型的数据
}
```

## 4. 认证流程

1. 调用 `/auth/login` 接口进行登录，传入用户名和密码
2. 登录成功后，服务器会在响应中设置Session Cookie
3. 后续请求会自动携带Session Cookie，服务器通过Session验证用户身份
4. 调用 `/auth/logout` 接口退出登录，服务器会销毁Session

## 5. 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 401 | 未登录或登录超时 |
| 403 | 权限不足 |
| 500 | 服务器内部错误 |
| 其他 | 具体错误信息见message字段 |

## 6. 测试工具推荐

1. **Postman**：用于发送HTTP请求，测试API接口
2. **Swagger UI**：访问地址 http://localhost:8080/metadata-system/swagger-ui.html，可在线查看API文档和测试API
3. **curl**：命令行工具，用于发送HTTP请求

## 7. 测试注意事项

1. 测试前确保服务已启动，数据库连接正常
2. 测试时注意请求参数的格式和类型
3. 测试完成后，及时清理测试数据
4. 对于修改、删除等操作，需谨慎操作，避免影响生产数据
5. 测试时注意观察响应状态码和响应消息，