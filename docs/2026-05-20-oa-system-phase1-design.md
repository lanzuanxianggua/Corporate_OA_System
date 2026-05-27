# 企业OA系统 - 第一阶段设计文档：基础平台 + 组织架构与权限

## 1. 项目概述

### 1.1 项目定位

面向真实企业商业开发的OA系统，采用前后端分离架构，微服务体系。第一阶段聚焦基础设施和组织架构权限模块，为后续业务模块奠定底座。

### 1.2 第一阶段范围

```
第一阶段：基础平台 + 组织架构与权限
├── 基础设施
│   ├── 数据字典管理
│   ├── 系统参数配置
│   ├── 多通道消息通知（站内信为主，预留短信/邮件通道）
│   ├── 操作审计日志
│   ├── 导入导出引擎
│   └── 定时任务调度（预留XXL-JOB接口）
│
├── 组织架构
│   ├── 公司/部门管理（多级树形结构）
│   ├── 岗位管理
│   ├── 用户管理（含兼职、代理关系）
│   ├── 组织变更历史
│   └── 离职/冻结/交接
│
├── 权限体系
│   ├── RBAC功能权限（菜单 + 按钮 + 接口级别）
│   ├── 数据权限（Data Scope）
│   ├── JWT + Refresh Token认证
│   └── 多租户架构预留
│
└── 系统管理
    ├── 登录日志
    ├── 在线用户管理
    ├── 密码策略
    └── 系统监控仪表盘（预留）
```

### 1.3 成功标准

- 用户能通过JWT登录，获取动态菜单和按钮权限
- 管理员能完整管理组织架构（公司-部门-岗位-用户）
- 角色能绑定菜单权限和数据权限，数据查询按Data Scope自动过滤
- 所有敏感操作自动记录审计日志
- 数据字典和系统配置可动态管理，无需重启

## 2. 技术选型

### 2.1 后端

| 组件 | 选型 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.x |
| 微服务框架 | Spring Cloud Alibaba | 2022.x |
| JDK | JDK 17 | - |
| 注册/配置中心 | Nacos | 2.x |
| API网关 | Spring Cloud Gateway | - |
| 数据库 | MySQL | 8.0 |
| ORM | MyBatis-Plus | 3.5.x |
| 缓存 | Redis | 7.x |
| 认证 | JWT (jjwt) | 0.12.x |
| API文档 | Knife4j | 4.x |
| 连接池 | HikariCP | - |
| 对象映射 | MapStruct | 1.5.x |

### 2.2 前端

| 组件 | 选型 | 版本 |
|------|------|------|
| 核心框架 | Vue 3 + TypeScript | 3.4.x |
| UI框架 | Element Plus | 2.x |
| 构建工具 | Vite | 5.x |
| 状态管理 | Pinia | 2.x |
| 路由 | Vue Router | 4.x |
| HTTP | Axios | 1.x |
| 样式 | SCSS + CSS Variables | - |

### 2.3 基础设施

| 组件 | 选型 |
|------|------|
| 注册/配置中心 | Nacos 2.x |
| 容器化 | Docker + Docker Compose（开发环境） |
| 反向代理 | Nginx |

## 3. 系统架构

### 3.1 微服务架构图

```
                        ┌──────────────┐
                        │    Nginx     │
                        │  反向代理     │
                        └──────┬───────┘
                               │
                        ┌──────▼───────┐
                        │ Spring Cloud │
                        │   Gateway    │
                        │  路由/鉴权    │
                        └──────┬───────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
  ┌───────▼───────┐  ┌────────▼───────┐  ┌────────▼───────┐
  │  oa-system    │  │  oa-auth       │  │  oa-business   │
  │  系统管理服务   │  │  认证授权服务   │  │  业务服务       │
  │               │  │               │  │ (后续扩展)      │
  │ ·用户管理      │  │ ·登录/登出     │  │               │
  │ ·部门管理      │  │ ·JWT签发/刷新  │  │               │
  │ ·岗位管理      │  │ ·权限校验      │  │               │
  │ ·角色管理      │  │ ·在线用户管理   │  │               │
  │ ·菜单/权限     │  │               │  │               │
  │ ·数据字典      │  │               │  │               │
  │ ·操作日志      │  │               │  │               │
  │ ·登录日志      │  │               │  │               │
  │ ·通知公告      │  │               │  │               │
  │ ·系统配置      │  │               │  │               │
  └───────┬───────┘  └────────┬───────┘  └────────┬───────┘
          │                    │                    │
          └────────────────────┼────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
  ┌───────▼───────┐  ┌────────▼───────┐  ┌────────▼───────┐
  │  Nacos        │  │  MySQL 8.0     │  │  Redis         │
  │  注册/配置中心  │  │  主从复制       │  │  Token/缓存     │
  └───────────────┘  └────────────────┘  └────────────────┘
```

### 3.2 服务职责

| 服务 | 职责 | 端口 |
|------|------|------|
| oa-gateway | API路由分发、统一鉴权、限流、黑白名单 | 8080 |
| oa-auth | 登录认证、JWT签发/刷新/校验、在线用户管理 | 9200 |
| oa-system | 组织架构、权限、字典、日志、公告、配置管理 | 9201 |
| oa-common | 公共模块（非独立服务），被其他服务以jar依赖引用 | - |

### 3.3 oa-common子模块

| 子模块 | 职责 |
|--------|------|
| oa-common-core | 统一响应体R、分页体TableDataInfo、全局异常处理、通用工具类、常量定义 |
| oa-common-security | JWT工具类、权限注解@PreAuth、SecurityContext工具 |
| oa-common-log | @OperLog注解 + AOP切面，自动记录操作日志 |
| oa-common-mybatis | MyBatis-Plus配置、@DataScope数据权限拦截器、自动填充（createBy/updateBy/createTime/updateTime）、逻辑删除 |
| oa-common-redis | RedisTemplate封装、缓存工具、分布式锁 |
| oa-common-swagger | Knife4j统一配置 |

## 4. 数据库设计

### 4.1 oa_auth库

```sql
-- 用户账号表
CREATE TABLE sys_user (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    dept_id       BIGINT       DEFAULT NULL COMMENT '部门ID',
    username      VARCHAR(30)  NOT NULL COMMENT '用户名',
    nick_name     VARCHAR(30)  NOT NULL COMMENT '昵称',
    email         VARCHAR(50)  DEFAULT '' COMMENT '邮箱',
    phone         VARCHAR(20)  DEFAULT '' COMMENT '手机号',
    sex           CHAR(1)      DEFAULT '0' COMMENT '性别(0男 1女 2未知)',
    avatar        VARCHAR(200) DEFAULT '' COMMENT '头像URL',
    password      VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1停用)',
    login_ip      VARCHAR(128) DEFAULT '' COMMENT '最后登录IP',
    login_date    DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    pwd_update_date DATETIME   DEFAULT NULL COMMENT '密码最后修改时间',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark        VARCHAR(500) DEFAULT '' COMMENT '备注',
    del_flag      CHAR(1)      DEFAULT '0' COMMENT '删除标志(0存在 1删除)',
    tenant_id     BIGINT       DEFAULT 0 COMMENT '租户ID(预留)',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='用户信息表';

-- 在线用户表
CREATE TABLE sys_user_online (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    token_id      VARCHAR(64)  NOT NULL COMMENT 'Token标识',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    user_name     VARCHAR(30)  NOT NULL COMMENT '用户名',
    dept_name     VARCHAR(50)  DEFAULT '' COMMENT '部门名称',
    ip_addr       VARCHAR(128) DEFAULT '' COMMENT '登录IP',
    login_location VARCHAR(255) DEFAULT '' COMMENT '登录地点',
    browser       VARCHAR(50)  DEFAULT '' COMMENT '浏览器',
    os            VARCHAR(50)  DEFAULT '' COMMENT '操作系统',
    expire_time   DATETIME     NOT NULL COMMENT '过期时间',
    create_time   DATETIME     DEFAULT NULL COMMENT '登录时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_token (token_id)
) ENGINE=InnoDB COMMENT='在线用户表';

-- 登录日志表
CREATE TABLE sys_login_log (
    info_id       BIGINT       NOT NULL AUTO_INCREMENT,
    user_name     VARCHAR(50)  DEFAULT '' COMMENT '用户名',
    ip_addr       VARCHAR(128) DEFAULT '' COMMENT '登录IP',
    login_location VARCHAR(255) DEFAULT '' COMMENT '登录地点',
    browser       VARCHAR(50)  DEFAULT '' COMMENT '浏览器',
    os            VARCHAR(50)  DEFAULT '' COMMENT '操作系统',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态(0成功 1失败)',
    msg           VARCHAR(255) DEFAULT '' COMMENT '提示消息',
    login_time    DATETIME     DEFAULT NULL COMMENT '登录时间',
    PRIMARY KEY (info_id)
) ENGINE=InnoDB COMMENT='登录日志表';
```

### 4.2 oa_system库

```sql
-- 部门表
CREATE TABLE sys_dept (
    dept_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    parent_id     BIGINT       DEFAULT 0 COMMENT '父部门ID',
    ancestors     VARCHAR(255) DEFAULT '' COMMENT '祖级列表(逗号分隔)',
    dept_name     VARCHAR(30)  NOT NULL COMMENT '部门名称',
    order_num     INT          DEFAULT 0 COMMENT '显示顺序',
    leader        VARCHAR(20)  DEFAULT '' COMMENT '负责人',
    phone         VARCHAR(20)  DEFAULT '' COMMENT '联系电话',
    email         VARCHAR(50)  DEFAULT '' COMMENT '邮箱',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1停用)',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    del_flag      CHAR(1)      DEFAULT '0' COMMENT '删除标志',
    tenant_id     BIGINT       DEFAULT 0 COMMENT '租户ID(预留)',
    PRIMARY KEY (dept_id)
) ENGINE=InnoDB COMMENT='部门表';

-- 岗位表
CREATE TABLE sys_post (
    post_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    post_code     VARCHAR(64)  NOT NULL COMMENT '岗位编码',
    post_name     VARCHAR(50)  NOT NULL COMMENT '岗位名称',
    order_num     INT          DEFAULT 0 COMMENT '显示顺序',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1停用)',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark        VARCHAR(500) DEFAULT '' COMMENT '备注',
    del_flag      CHAR(1)      DEFAULT '0' COMMENT '删除标志',
    tenant_id     BIGINT       DEFAULT 0 COMMENT '租户ID(预留)',
    PRIMARY KEY (post_id)
) ENGINE=InnoDB COMMENT='岗位表';

-- 用户-岗位关联表
CREATE TABLE sys_user_post (
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    post_id       BIGINT       NOT NULL COMMENT '岗位ID',
    PRIMARY KEY (user_id, post_id)
) ENGINE=InnoDB COMMENT='用户-岗位关联表';

-- 角色表
CREATE TABLE sys_role (
    role_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_name     VARCHAR(30)  NOT NULL COMMENT '角色名称',
    role_key      VARCHAR(100) NOT NULL COMMENT '角色权限字符',
    role_sort     INT          DEFAULT 0 COMMENT '显示顺序',
    data_scope    CHAR(1)      DEFAULT '1' COMMENT '数据范围(1全部 2本部门 3本部门及以下 4仅本人 5自定义)',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1停用)',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark        VARCHAR(500) DEFAULT '' COMMENT '备注',
    del_flag      CHAR(1)      DEFAULT '0' COMMENT '删除标志',
    tenant_id     BIGINT       DEFAULT 0 COMMENT '租户ID(预留)',
    PRIMARY KEY (role_id)
) ENGINE=InnoDB COMMENT='角色信息表';

-- 用户-角色关联表
CREATE TABLE sys_user_role (
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    role_id       BIGINT       NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB COMMENT='用户-角色关联表';

-- 菜单/权限表
CREATE TABLE sys_menu (
    menu_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    menu_name     VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    parent_id     BIGINT       DEFAULT 0 COMMENT '父菜单ID',
    order_num     INT          DEFAULT 0 COMMENT '显示顺序',
    path          VARCHAR(200) DEFAULT '' COMMENT '路由地址',
    component     VARCHAR(255) DEFAULT '' COMMENT '组件路径',
    query         VARCHAR(255) DEFAULT '' COMMENT '路由参数',
    route_name    VARCHAR(50)  DEFAULT '' COMMENT '路由名称',
    is_frame      CHAR(1)      DEFAULT '1' COMMENT '是否外链(0是 1否)',
    is_cache      CHAR(1)      DEFAULT '0' COMMENT '是否缓存(0缓存 1不缓存)',
    menu_type     CHAR(1)      DEFAULT 'M' COMMENT '类型(M目录 C菜单 F按钮)',
    visible       CHAR(1)      DEFAULT '0' COMMENT '显示状态(0显示 1隐藏)',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1停用)',
    perms         VARCHAR(100) DEFAULT '' COMMENT '权限标识',
    icon          VARCHAR(100) DEFAULT '' COMMENT '菜单图标',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark        VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (menu_id)
) ENGINE=InnoDB COMMENT='菜单权限表';

-- 角色-菜单关联表
CREATE TABLE sys_role_menu (
    role_id       BIGINT       NOT NULL COMMENT '角色ID',
    menu_id       BIGINT       NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB COMMENT='角色-菜单关联表';

-- 角色-部门关联表(数据权限-自定义)
CREATE TABLE sys_role_dept (
    role_id       BIGINT       NOT NULL COMMENT '角色ID',
    dept_id       BIGINT       NOT NULL COMMENT '部门ID',
    PRIMARY KEY (role_id, dept_id)
) ENGINE=InnoDB COMMENT='角色-部门关联表';

-- 字典类型表
CREATE TABLE sys_dict_type (
    dict_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典主键',
    dict_name     VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_type     VARCHAR(100) NOT NULL COMMENT '字典类型',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1停用)',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark        VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (dict_id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB COMMENT='字典类型表';

-- 字典数据表
CREATE TABLE sys_dict_data (
    dict_code     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典编码',
    dict_type     VARCHAR(100) NOT NULL COMMENT '字典类型',
    dict_label    VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value    VARCHAR(100) NOT NULL COMMENT '字典键值',
    dict_sort     INT          DEFAULT 0 COMMENT '排序',
    css_class     VARCHAR(100) DEFAULT '' COMMENT '样式属性',
    list_class    VARCHAR(100) DEFAULT '' COMMENT '表格回显样式(default/primary/success/info/warning/danger)',
    is_default    CHAR(1)      DEFAULT 'N' COMMENT '是否默认(Y是 N否)',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1停用)',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark        VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (dict_code)
) ENGINE=InnoDB COMMENT='字典数据表';

-- 系统配置表
CREATE TABLE sys_config (
    config_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '参数主键',
    config_name   VARCHAR(100) NOT NULL COMMENT '参数名称',
    config_key    VARCHAR(100) NOT NULL COMMENT '参数键名',
    config_value  VARCHAR(500) NOT NULL COMMENT '参数键值',
    config_type   CHAR(1)      DEFAULT 'N' COMMENT '系统内置(Y是 N否)',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark        VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (config_id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB COMMENT='参数配置表';

-- 操作日志表
CREATE TABLE sys_oper_log (
    oper_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    title         VARCHAR(50)  DEFAULT '' COMMENT '操作模块',
    business_type TINYINT      DEFAULT 0 COMMENT '业务类型(0其它 1新增 2修改 3删除)',
    method        VARCHAR(200) DEFAULT '' COMMENT '方法名称',
    request_method VARCHAR(10) DEFAULT '' COMMENT '请求方式',
    operator_type TINYINT      DEFAULT 0 COMMENT '操作类别(0其它 1后台用户 2手机端用户)',
    oper_name     VARCHAR(50)  DEFAULT '' COMMENT '操作人员',
    oper_url      VARCHAR(255) DEFAULT '' COMMENT '请求URL',
    oper_ip       VARCHAR(128) DEFAULT '' COMMENT '主机地址',
    oper_location VARCHAR(255) DEFAULT '' COMMENT '操作地点',
    oper_param    TEXT         DEFAULT NULL COMMENT '请求参数',
    json_result   TEXT         DEFAULT NULL COMMENT '返回参数',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1异常)',
    error_msg     TEXT         DEFAULT NULL COMMENT '错误消息',
    oper_time     DATETIME     DEFAULT NULL COMMENT '操作时间',
    cost_time     BIGINT       DEFAULT 0 COMMENT '消耗时间(ms)',
    PRIMARY KEY (oper_id),
    KEY idx_oper_time (oper_time)
) ENGINE=InnoDB COMMENT='操作日志表';

-- 通知公告表
CREATE TABLE sys_notice (
    notice_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    notice_title    VARCHAR(100) NOT NULL COMMENT '公告标题',
    notice_type     CHAR(1)      NOT NULL COMMENT '公告类型(1通知 2公告)',
    notice_content  TEXT         DEFAULT NULL COMMENT '公告内容',
    status          CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1关闭)',
    create_by       VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time     DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_by       VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time     DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark          VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (notice_id)
) ENGINE=InnoDB COMMENT='通知公告表';

-- 用户-公告阅读表
CREATE TABLE sys_user_notice (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    notice_id     BIGINT       NOT NULL COMMENT '公告ID',
    read_flag     CHAR(1)      DEFAULT '0' COMMENT '阅读状态(0未读 1已读)',
    read_time     DATETIME     DEFAULT NULL COMMENT '阅读时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_notice (user_id, notice_id)
) ENGINE=InnoDB COMMENT='用户-公告阅读表';
```

## 5. API设计

### 5.1 统一规范

- 路径前缀：`/api`
- 响应格式：`{ "code": 200, "msg": "操作成功", "data": {} }`
- 分页响应：`{ "code": 200, "msg": "操作成功", "rows": [], "total": 100 }`
- 认证方式：`Authorization: Bearer {accessToken}`
- 业务码：200成功, 401未认证, 403无权限, 500业务异常

### 5.2 认证服务API (oa-auth)

```
POST   /api/auth/login              # 登录（username+password+code+uuid）
POST   /api/auth/logout             # 登出
POST   /api/auth/refresh            # 刷新Token
GET    /api/auth/captcha            # 获取验证码
GET    /api/auth/info               # 获取当前用户信息+权限+菜单
```

### 5.3 系统管理API (oa-system)

```
# 用户管理
GET    /api/system/user/list        # 分页查询
GET    /api/system/user/{id}        # 详情
POST   /api/system/user             # 新增
PUT    /api/system/user             # 修改
DELETE /api/system/user/{ids}       # 删除（批量）
PUT    /api/system/user/resetPwd    # 重置密码
PUT    /api/system/user/status      # 改变状态
POST   /api/system/user/profile     # 修改个人信息
POST   /api/system/user/avatar      # 上传头像
GET    /api/system/user/profile     # 获取个人信息

# 部门管理
GET    /api/system/dept/list        # 列表（前端构建树）
GET    /api/system/dept/{id}        # 详情
POST   /api/system/dept             # 新增
PUT    /api/system/dept             # 修改
DELETE /api/system/dept/{id}        # 删除

# 岗位管理
GET    /api/system/post/list        # 分页查询
GET    /api/system/post/{id}        # 详情
POST   /api/system/post             # 新增
PUT    /api/system/post             # 修改
DELETE /api/system/post/{ids}       # 删除

# 角色管理
GET    /api/system/role/list        # 分页查询
GET    /api/system/role/{id}        # 详情
POST   /api/system/role             # 新增
PUT    /api/system/role             # 修改
DELETE /api/system/role/{ids}       # 删除
PUT    /api/system/role/dataScope   # 修改数据权限

# 菜单管理
GET    /api/system/menu/list        # 列表
GET    /api/system/menu/{id}        # 详情
POST   /api/system/menu             # 新增
PUT    /api/system/menu             # 修改
DELETE /api/system/menu/{id}        # 删除

# 数据字典
GET    /api/system/dict/type/list   # 字典类型分页
GET    /api/system/dict/type/{id}   # 字典类型详情
POST   /api/system/dict/type        # 新增字典类型
PUT    /api/system/dict/type        # 修改字典类型
DELETE /api/system/dict/type/{ids}  # 删除字典类型
GET    /api/system/dict/data/list   # 字典数据分页
GET    /api/system/dict/data/{id}   # 字典数据详情
POST   /api/system/dict/data        # 新增字典数据
PUT    /api/system/dict/data        # 修改字典数据
DELETE /api/system/dict/data/{ids}  # 删除字典数据
GET    /api/system/dict/data/type/{dictType}  # 按类型查询字典数据（前端下拉框用）

# 系统配置
GET    /api/system/config/list      # 分页查询
GET    /api/system/config/{id}      # 详情
GET    /api/system/config/key/{key} # 按key查询
POST   /api/system/config           # 新增
PUT    /api/system/config           # 修改
DELETE /api/system/config/{ids}     # 删除

# 通知公告
GET    /api/system/notice/list      # 分页查询
GET    /api/system/notice/{id}      # 详情
POST   /api/system/notice           # 新增
PUT    /api/system/notice           # 修改
DELETE /api/system/notice/{ids}     # 删除

# 操作日志
GET    /api/system/operlog/list     # 分页查询
DELETE /api/system/operlog/{ids}    # 删除
DELETE /api/system/operlog/clean    # 清空

# 登录日志
GET    /api/system/loginlog/list    # 分页查询
DELETE /api/system/loginlog/{ids}   # 删除
DELETE /api/system/loginlog/clean   # 清空

# 在线用户
GET    /api/system/online/list      # 分页查询
DELETE /api/system/online/{tokenId} # 强制下线
```

## 6. 安全机制

### 6.1 JWT双Token认证

```
登录 → auth服务校验账号密码+验证码
    → 签发 AccessToken(30min) + RefreshToken(7天)
    → 存入Redis: login:token:{uuid} 和 login:refresh:{uuid}
    → 前端存储到localStorage

请求 → Gateway过滤器:
    1. 校验Token签名和有效期
    2. 从Redis确认Token未被吊销
    3. 解析userId/username/roles
    4. 放入请求Header传递给下游服务

Token过期 → 前端Axios拦截器自动用RefreshToken静默刷新
双Token都过期 → 跳转登录页
```

### 6.2 数据权限拦截器

通过 `@DataScope(deptAlias = "d")` 注解标记需要数据权限过滤的Mapper方法，MyBatis拦截器自动拼接SQL：

| data_scope | 含义 | SQL过滤 |
|------------|------|---------|
| 1 | 全部数据 | 无过滤 |
| 2 | 本部门 | AND d.dept_id = {currentUser.deptId} |
| 3 | 本部门及以下 | AND d.dept_id IN ({本部门及子部门IDs}) |
| 4 | 仅本人 | AND u.create_by = {currentUser.userId} |
| 5 | 自定义 | AND d.dept_id IN ({sys_role_dept关联的部门IDs}) |

### 6.3 操作审计日志

通过 `@OperLog(title, businessType)` 注解声明式记录，AOP切面自动采集：操作模块、类型、方法、请求参数、返回结果、操作人、IP、耗时、异常信息。

### 6.4 密码策略

- BCrypt加密存储
- 最小8位，含大小写+数字+特殊字符
- 初始密码强制修改
- 错误5次锁定30分钟（Redis计数）
- 密码过期策略（可配置天数）

### 6.5 接口级权限

菜单表的 `perms` 字段存储权限标识（如 `system:user:list`、`system:user:add`），通过 `@PreAuth("system:user:list")` 注解在Controller方法上控制访问权限。

## 7. 前端架构

### 7.1 布局

现代企业后台风格：
- 左侧：可折叠侧边菜单（多级展开）
- 顶部：面包屑 + 标签页导航 + 用户信息 + 退出
- 内容区：搜索表单 + 操作按钮 + 数据表格 + 分页

### 7.2 动态路由

1. 用户登录后，前端请求 `/api/auth/info` 获取用户权限和菜单数据
2. 后端根据用户角色查询关联的菜单（menu_type = M或C），构建路由树
3. 前端通过 `router.addRoute()` 动态注册路由
4. 按钮权限通过自定义指令 `v-hasPermi="['system:user:add']"` 控制

### 7.3 关键交互流程

**登录流程：**
```
登录页 → 输入账号密码+验证码
  → POST /api/auth/login
  → 存储accessToken/refreshToken到localStorage
  → GET /api/auth/info 获取用户信息+菜单+权限
  → 生成动态路由，跳转首页
```

**CRUD表格通用模式：**
```
页面加载 → 搜索表单(可选) + 数据表格 + 分页
  → GET /api/xxx/list?pageNum=1&pageSize=10&查询条件
  → 新增/编辑 → 弹窗表单 → POST/PUT /api/xxx
  → 删除 → 确认弹窗 → DELETE /api/xxx/{ids}
  → 表格数据刷新
```

## 8. 项目结构

### 8.1 后端

```
code/backend/
├── pom.xml
├── oa-common/
│   ├── oa-common-core/
│   ├── oa-common-security/
│   ├── oa-common-log/
│   ├── oa-common-mybatis/
│   ├── oa-common-redis/
│   └── oa-common-swagger/
├── oa-gateway/
│   └── src/main/java/.../gateway/
│       ├── config/
│       ├── filter/
│       ├── handler/
│       └── properties/
├── oa-auth/
│   └── src/main/java/.../auth/
│       ├── controller/
│       ├── service/
│       └── domain/
├── oa-system/
│   └── src/main/java/.../system/
│       ├── controller/
│       ├── service/
│       ├── mapper/
│       └── domain/
└── sql/
    ├── oa_auth.sql
    └── oa_system.sql
```

### 8.2 前端

```
code/frontend/
├── package.json / vite.config.ts / tsconfig.json
├── .env.development / .env.production
├── public/
└── src/
    ├── api/           # 接口请求（按模块分文件）
    ├── assets/        # 静态资源、样式
    ├── components/    # 通用组件
    ├── composables/   # 组合式函数
    ├── layout/        # 布局组件
    ├── router/        # 路由（静态+动态）
    ├── store/         # Pinia状态管理
    ├── utils/         # 工具函数
    └── views/         # 页面视图
        ├── login/
        ├── redirect/
        ├── error/
        └── system/    # 系统管理页面
```
