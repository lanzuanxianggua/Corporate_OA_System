# 企业OA系统 - 第二阶段设计文档：核心办公模块

## 1. 阶段概述

第二阶段构建OA系统的核心办公能力，包含流程审批引擎、文档与知识管理、沟通与协作三个模块。这三个模块是企业日常办公中使用频率最高的功能，也是区分"能用"和"好用"的关键。

### 1.1 依赖关系

```
第一阶段（已完成）          第二阶段
┌─────────────────┐      ┌──────────────────────┐
│ 组织架构与权限    │◄─────│ 流程审批引擎          │
│ 数据字典         │◄─────│ 文档与知识管理         │
│ 系统配置         │◄─────│ 沟通与协作            │
│ 操作审计日志     │◄─────│ 待办中心             │
│ 多通道消息通知   │◄─────│                      │
└─────────────────┘      └──────────────────────┘
```

### 1.2 新增微服务

| 服务 | 职责 | 端口 |
|------|------|------|
| oa-workflow | 流程审批引擎服务 | 9202 |
| oa-document | 文档与知识管理服务 | 9203 |

沟通与协作功能中，消息通知复用第一阶段的系统服务，IM消息独立为轻量WebSocket服务集成在网关层。

## 2. 流程审批引擎

流程审批是OA系统的心脏，占日常使用频率的60%以上。

### 2.1 核心能力

#### 2.1.1 流程设计器（可视化）

基于BPMN 2.0规范，提供Web端可视化流程设计器：

**节点类型：**
| 节点 | 说明 | 配置项 |
|------|------|--------|
| 开始节点 | 流程入口，只能有一个 | 发起人范围（哪些角色可以发起） |
| 审批节点 | 一个人或多人审批 | 审批人（指定人/角色/部门主管/发起人主管）、审批类型（或签/会签/依次审批）、超时设置 |
| 条件分支 | 根据条件走不同路径 | 条件表达式（金额>5000、部门=XX）、优先级 |
| 抄送节点 | 知会相关人，不需审批 | 抄送人（指定人/角色/部门）、是否允许查看详情 |
| 结束节点 | 流程结束 | - |

**审批人设置策略：**
- 指定成员：直接选择具体人员
- 指定角色：选择角色，该角色下所有人可审批
- 部门主管：取发起人所在部门的主管
- 发起人主管：取发起人的直属上级
| 表单内联系人：从表单中某个字段取值
- 连续多级主管：从发起人起往上N级主管

**条件分支表达式：**
```
支持条件：
- 表单字段比较：amount > 5000, dept == "财务部"
- 发起人属性：initiator.deptLevel == 2
- 组合条件：amount > 5000 AND dept == "技术部"
```

#### 2.1.2 表单设计器

与流程设计器配合的表单设计器，支持可视化拖拽：

**表单控件：**
| 控件 | 数据类型 | 说明 |
|------|---------|------|
| 单行文本 | VARCHAR | 标题、名称等 |
| 多行文本 | TEXT | 描述、说明等 |
| 数字输入 | DECIMAL | 金额、数量 |
| 日期选择 | DATE | 日期 |
| 日期时间 | DATETIME | 精确到时分 |
| 下拉选择 | VARCHAR | 关联字典或自定义选项 |
| 单选/多选 | VARCHAR | 关联字典或自定义选项 |
| 人员选择 | VARCHAR | 关联用户表 |
| 部门选择 | VARCHAR | 关联部门表 |
| 文件上传 | VARCHAR | 关联文件存储 |
| 金额 | DECIMAL | 带大写转换的金额输入 |
| 明细表 | JSON | 子表（报销明细、采购清单等） |
| 计算公式 | - | 根据其他字段自动计算（如合计=单价*数量） |

**表单与流程联动：**
- 条件分支的判断条件基于表单字段值
- 审批节点的审批人可基于表单字段（如表单中选的"项目负责人"）
- 表单字段的读写权限可在每个审批节点独立控制（可编辑/只读/隐藏）

#### 2.1.3 流程分类管理

```
流程分类树：
├── 行政管理
│   ├── 请假申请
│   ├── 加班申请
│   ├── 出差申请
│   ├── 用车申请
│   └── 印章使用申请
├── 人事管理
│   ├── 招聘需求
│   ├── 入职办理
│   ├── 离职申请
│   └── 转正申请
├── 财务管理
│   ├── 费用报销
│   ├── 付款申请
│   ├── 借款申请
│   └── 采购申请
├── 合同管理
│   ├── 合同审批
│   └── 合同变更
└── 通用审批
    ├── 通用审批单
    └── 信息发布审批
```

每个流程分类可配置：编码前缀、谁可以发起、是否启用、排序。

#### 2.1.4 流程版本管理

- 流程定义每次修改发布产生新版本
- 版本号自动递增（v1, v2, v3...）
- 已发起的流程实例继续使用发起时的版本运行
- 新发起的流程使用最新版本
- 支持查看历史版本和版本差异对比
- 支持回滚到历史版本（创建新版本，内容为目标版本）

#### 2.1.5 审批操作

| 操作 | 说明 | 权限 |
|------|------|------|
| 同意 | 通过当前节点 | 当前审批人 |
| 拒绝 | 驳回流程（可配置驳回到发起人/上一节点/指定节点） | 当前审批人 |
| 转办 | 将任务转给其他人处理 | 当前审批人 |
| 加签 | 增加一个临时审批人（前加签/后加签） | 当前审批人 |
| 减签 | 减少会签中的审批人 | 当前审批人（会签场景） |
| 撤回 | 发起人撤回已提交的流程（仅限下一节点未处理时） | 发起人 |
| 催办 | 提醒当前审批人尽快处理 | 发起人 |
| 暂存 | 保存审批意见但不提交 | 当前审批人 |
| 评论 | 在流程中添加评论/意见 | 有权限查看的人 |

#### 2.1.6 流程监控与管理

**管理视图：**
- 全部流程实例列表（按分类/状态/时间筛选）
- 流程实例详情：表单数据 + 审批流转图（高亮当前节点） + 审批历史
- 流程图实时状态：每个节点的审批人、时间、结果、意见

**异常处理（管理员）：**
- 指派：将挂起的任务指派给其他人
- 跳转：跳过某个节点
- 终止：强制终止流程
- 挂起/激活：暂停/恢复某个流程实例
- 变更审批人：修改待办节点的审批人

#### 2.1.7 催办与超时机制

- 流程设计时配置超时时间（如：审批节点超过24小时未处理）
- 超时自动提醒：到期前提醒、到期后每小时提醒、超期升级（提醒审批人的上级）
- 发起人可手动催办（发送站内信/短信通知审批人）
- 催办记录留存

#### 2.1.8 流程数据分析

- 流程效率统计：平均审批时长、各节点平均停留时间
- 部门效率排名：各部门流程处理时效排名
- 瓶颈分析：找出平均处理时间最长的节点
- 发起统计：各流程类型的发起量趋势
- 逾期统计：逾期流程数量和占比

### 2.2 数据库设计（oa_workflow库）

```sql
-- 流程分类
CREATE TABLE wf_category (
    category_id   BIGINT       NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    category_code VARCHAR(50)  NOT NULL COMMENT '分类编码',
    parent_id     BIGINT       DEFAULT 0 COMMENT '父分类ID',
    order_num     INT          DEFAULT 0 COMMENT '排序',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态',
    create_by     VARCHAR(64)  DEFAULT '',
    create_time   DATETIME     DEFAULT NULL,
    update_by     VARCHAR(64)  DEFAULT '',
    update_time   DATETIME     DEFAULT NULL,
    del_flag      CHAR(1)      DEFAULT '0',
    tenant_id     BIGINT       DEFAULT 0,
    PRIMARY KEY (category_id)
) ENGINE=InnoDB COMMENT='流程分类表';

-- 流程定义（每个版本一条记录）
CREATE TABLE wf_process_definition (
    definition_id   BIGINT       NOT NULL AUTO_INCREMENT,
    process_key     VARCHAR(64)  NOT NULL COMMENT '流程标识',
    process_name    VARCHAR(100) NOT NULL COMMENT '流程名称',
    category_id     BIGINT       NOT NULL COMMENT '分类ID',
    version         INT          DEFAULT 1 COMMENT '版本号',
    form_id         BIGINT       COMMENT '关联表单ID',
    process_xml     LONGTEXT     NOT NULL COMMENT 'BPMN XML',
    process_json    LONGTEXT     COMMENT '流程设计器JSON（用于前端渲染）',
    description     VARCHAR(500) DEFAULT '' COMMENT '描述',
    status          CHAR(1)      DEFAULT '0' COMMENT '状态(0启用 1停用)',
    suspension_state TINYINT    DEFAULT 1 COMMENT '挂起状态(1激活 2挂起)',
    create_by       VARCHAR(64)  DEFAULT '',
    create_time     DATETIME     DEFAULT NULL,
    update_by       VARCHAR(64)  DEFAULT '',
    update_time     DATETIME     DEFAULT NULL,
    del_flag        CHAR(1)      DEFAULT '0',
    tenant_id       BIGINT       DEFAULT 0,
    PRIMARY KEY (definition_id),
    KEY idx_process_key_version (process_key, version)
) ENGINE=InnoDB COMMENT='流程定义表';

-- 表单定义
CREATE TABLE wf_form_definition (
    form_id       BIGINT       NOT NULL AUTO_INCREMENT,
    form_name     VARCHAR(100) NOT NULL COMMENT '表单名称',
    form_json     LONGTEXT     NOT NULL COMMENT '表单设计器JSON',
    form_version  INT          DEFAULT 1 COMMENT '版本号',
    description   VARCHAR(500) DEFAULT '',
    status        CHAR(1)      DEFAULT '0',
    create_by     VARCHAR(64)  DEFAULT '',
    create_time   DATETIME     DEFAULT NULL,
    update_by     VARCHAR(64)  DEFAULT '',
    update_time   DATETIME     DEFAULT NULL,
    del_flag      CHAR(1)      DEFAULT '0',
    tenant_id     BIGINT       DEFAULT 0,
    PRIMARY KEY (form_id)
) ENGINE=InnoDB COMMENT='表单定义表';

-- 流程实例
CREATE TABLE wf_process_instance (
    instance_id     BIGINT       NOT NULL AUTO_INCREMENT,
    definition_id   BIGINT       NOT NULL COMMENT '流程定义ID',
    process_key     VARCHAR(64)  NOT NULL COMMENT '流程标识',
    process_name    VARCHAR(100) NOT NULL COMMENT '流程名称',
    form_id         BIGINT       COMMENT '表单ID',
    form_data       LONGTEXT     COMMENT '表单数据JSON',
    title           VARCHAR(200) NOT NULL COMMENT '流程标题',
    initiator_id    BIGINT       NOT NULL COMMENT '发起人ID',
    initiator_name  VARCHAR(30)  NOT NULL COMMENT '发起人姓名',
    initiator_dept  VARCHAR(100) DEFAULT '' COMMENT '发起人部门',
    status          CHAR(1)      DEFAULT '0' COMMENT '状态(0进行中 1已完成 2已终止 3已撤回)',
    start_time      DATETIME     NOT NULL COMMENT '发起时间',
    end_time        DATETIME     DEFAULT NULL COMMENT '结束时间',
    duration        BIGINT       DEFAULT 0 COMMENT '耗时(ms)',
    current_node    VARCHAR(100) DEFAULT '' COMMENT '当前节点名称',
    urgent_level    CHAR(1)      DEFAULT '0' COMMENT '紧急程度(0普通 1重要 2紧急)',
    create_by       VARCHAR(64)  DEFAULT '',
    create_time     DATETIME     DEFAULT NULL,
    update_by       VARCHAR(64)  DEFAULT '',
    update_time     DATETIME     DEFAULT NULL,
    del_flag        CHAR(1)      DEFAULT '0',
    tenant_id       BIGINT       DEFAULT 0,
    PRIMARY KEY (instance_id),
    KEY idx_initiator (initiator_id),
    KEY idx_status_time (status, start_time)
) ENGINE=InnoDB COMMENT='流程实例表';

-- 任务实例（每个审批节点的任务）
CREATE TABLE wf_task_instance (
    task_id         BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT       NOT NULL COMMENT '流程实例ID',
    definition_id   BIGINT       NOT NULL COMMENT '流程定义ID',
    node_id         VARCHAR(64)  NOT NULL COMMENT '节点ID',
    node_name       VARCHAR(100) NOT NULL COMMENT '节点名称',
    node_type       VARCHAR(20)  NOT NULL COMMENT '节点类型(approval/cc/condition)',
    assignee_id     BIGINT       COMMENT '处理人ID',
    assignee_name   VARCHAR(30)  COMMENT '处理人姓名',
    candidate_users VARCHAR(500) DEFAULT '' COMMENT '候选用户IDs(逗号分隔)',
    candidate_roles VARCHAR(500) DEFAULT '' COMMENT '候选角色IDs(逗号分隔)',
    task_type       CHAR(1)      DEFAULT '0' COMMENT '任务类型(0审批 1抄送)',
    status          CHAR(1)      DEFAULT '0' COMMENT '状态(0待处理 1已处理 2已转办 3已取消)',
    action          VARCHAR(20)  DEFAULT '' COMMENT '处理动作(approve/reject/transfer/countersign)',
    comment         TEXT         DEFAULT NULL COMMENT '审批意见',
    due_date        DATETIME     DEFAULT NULL COMMENT '到期时间',
    claim_time      DATETIME     DEFAULT NULL COMMENT '认领时间',
    complete_time   DATETIME     DEFAULT NULL COMMENT '完成时间',
    duration        BIGINT       DEFAULT 0 COMMENT '处理耗时(ms)',
    parent_task_id  BIGINT       DEFAULT NULL COMMENT '父任务(加签场景)',
    create_time     DATETIME     DEFAULT NULL,
    PRIMARY KEY (task_id),
    KEY idx_instance (instance_id),
    KEY idx_assignee_status (assignee_id, status)
) ENGINE=InnoDB COMMENT='任务实例表';

-- 流程操作记录
CREATE TABLE wf_operation_log (
    log_id         BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id    BIGINT       NOT NULL COMMENT '流程实例ID',
    task_id        BIGINT       DEFAULT NULL COMMENT '任务ID',
    operation      VARCHAR(20)  NOT NULL COMMENT '操作类型',
    operator_id    BIGINT       NOT NULL COMMENT '操作人ID',
    operator_name  VARCHAR(30)  NOT NULL COMMENT '操作人姓名',
    comment        TEXT         DEFAULT NULL COMMENT '意见',
    ext_data       TEXT         DEFAULT NULL COMMENT '扩展数据JSON',
    operate_time   DATETIME     NOT NULL COMMENT '操作时间',
    PRIMARY KEY (log_id),
    KEY idx_instance (instance_id)
) ENGINE=InnoDB COMMENT='流程操作记录表';

-- 流程委托设置
CREATE TABLE wf_delegation (
    delegation_id  BIGINT       NOT NULL AUTO_INCREMENT,
    delegator_id   BIGINT       NOT NULL COMMENT '委托人ID',
    delegate_id    BIGINT       NOT NULL COMMENT '被委托人ID',
    start_time     DATETIME     NOT NULL COMMENT '开始时间',
    end_time       DATETIME     NOT NULL COMMENT '结束时间',
    process_key    VARCHAR(64)  DEFAULT '' COMMENT '流程标识(空=全部流程)',
    status         CHAR(1)      DEFAULT '0' COMMENT '状态(0生效 1失效)',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    DATETIME     DEFAULT NULL,
    PRIMARY KEY (delegation_id)
) ENGINE=InnoDB COMMENT='流程委托表';

-- 流程催办记录
CREATE TABLE wf_urge_log (
    urge_id        BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id    BIGINT       NOT NULL COMMENT '流程实例ID',
    task_id        BIGINT       NOT NULL COMMENT '任务ID',
    urge_user_id   BIGINT       NOT NULL COMMENT '催办人ID',
    urge_user_name VARCHAR(30)  NOT NULL COMMENT '催办人姓名',
    urged_user_id  BIGINT       NOT NULL COMMENT '被催办人ID',
    urged_user_name VARCHAR(30) NOT NULL COMMENT '被催办人姓名',
    message        VARCHAR(500) DEFAULT '' COMMENT '催办消息',
    urge_time      DATETIME     NOT NULL,
    PRIMARY KEY (urge_id)
) ENGINE=InnoDB COMMENT='催办记录表';
```

### 2.3 API设计

```
# 流程分类
GET    /api/workflow/category/list
GET    /api/workflow/category/tree      # 树形结构
POST   /api/workflow/category
PUT    /api/workflow/category
DELETE /api/workflow/category/{id}

# 表单管理
GET    /api/workflow/form/list
GET    /api/workflow/form/{id}
POST   /api/workflow/form
PUT    /api/workflow/form
DELETE /api/workflow/form/{id}

# 流程定义
GET    /api/workflow/definition/list
GET    /api/workflow/definition/{id}
POST   /api/workflow/definition         # 创建/保存草稿
PUT    /api/workflow/definition/publish/{id}  # 发布
PUT    /api/workflow/definition/status   # 启用/停用
GET    /api/workflow/definition/versions/{processKey}  # 版本列表
DELETE /api/workflow/definition/{id}

# 流程发起
GET    /api/workflow/start/list         # 我可以发起的流程列表
GET    /api/workflow/start/form/{processKey}  # 获取流程表单配置
POST   /api/workflow/start              # 发起流程
POST   /api/workflow/start/draft        # 保存草稿
GET    /api/workflow/start/draft/list   # 我的草稿列表

# 我的流程
GET    /api/workflow/process/my       # 我发起的
GET    /api/workflow/process/todo     # 我的待办
GET    /api/workflow/process/done     # 我的已办
GET    /api/workflow/process/cc       # 抄送我的
GET    /api/workflow/process/{id}     # 流程详情（含表单+流程图+审批历史）

# 审批操作
POST   /api/workflow/task/approve/{taskId}    # 同意
POST   /api/workflow/task/reject/{taskId}     # 拒绝
POST   /api/workflow/task/transfer/{taskId}   # 转办
POST   /api/workflow/task/countersign/{taskId} # 加签
POST   /api/workflow/task/delegate/{taskId}   # 委派
POST   /api/workflow/task/withdraw/{instanceId} # 撤回
POST   /api/workflow/task/urge/{instanceId}   # 催办
POST   /api/workflow/task/comment/{instanceId} # 评论

# 流程管理（管理员）
GET    /api/workflow/admin/instances    # 全部流程实例
PUT    /api/workflow/admin/terminate/{instanceId}  # 终止
PUT    /api/workflow/admin/suspend/{instanceId}    # 挂起
PUT    /api/workflow/admin/activate/{instanceId}   # 激活
PUT    /api/workflow/admin/assign/{taskId}         # 指派
POST   /api/workflow/admin/jump/{instanceId}       # 跳转节点

# 流程统计
GET    /api/workflow/stats/efficiency   # 效率统计
GET    /api/workflow/stats/department   # 部门效率
GET    /api/workflow/stats/bottleneck   # 瓶颈分析
GET    /api/workflow/stats/trend        # 发起趋势

# 流程委托
GET    /api/workflow/delegation/list
POST   /api/workflow/delegation
PUT    /api/workflow/delegation
DELETE /api/workflow/delegation/{id}
```

## 3. 文档与知识管理

### 3.1 核心能力

#### 3.1.1 文档库结构

```
文档库
├── 公共文档库（全员可见）
│   ├── 公司制度
│   ├── 行政管理
│   ├── 人事管理
│   └── 财务管理
├── 部门文档库（按部门隔离）
│   ├── 技术部
│   ├── 市场部
│   └── 财务部
└── 项目文档库（按项目组织）
    ├── 项目A
    └── 项目B
```

**文件夹操作：** 创建、重命名、移动、删除、权限设置（继承父文件夹或独立设置）

#### 3.1.2 文件操作

| 功能 | 说明 |
|------|------|
| 上传 | 支持拖拽上传、批量上传、断点续传（大文件），限制文件类型和大小 |
| 下载 | 单文件/批量打包下载 |
| 在线预览 | Office(doc/xls/ppt)、PDF、图片、音视频、文本文件在线预览 |
| 在线编辑 | 集成OnlyOffice/WPS WebOffice，在线编辑Office文件 |
| 版本管理 | 每次修改自动产生新版本，可查看/对比/回退到历史版本 |
| 文件夹权限 | 继承父文件夹或独立设置，支持角色和用户级别 |

#### 3.1.3 知识管理

- **知识标签：** 为文档打标签，支持标签搜索和聚合
- **知识收藏：** 个人收藏夹，快速访问常用文档
- **知识推荐：** 基于浏览和下载记录推荐相关文档
- **知识审核：** 重要文档发布前需审核（关联流程引擎）
- **知识统计：** 文档浏览量、下载量、收藏量排行

#### 3.1.4 全文检索

- 基于Elasticsearch实现
- 支持文件名、文件内容（OCR图片文字）、标签、描述搜索
- 搜索结果高亮显示关键词
- 支持按文档类型、时间范围、上传者、部门筛选
- 支持搜索建议和搜索历史

#### 3.1.5 回收站

- 删除的文件进入回收站，30天后自动清理
- 回收站文件可恢复或永久删除
- 管理员可查看和清理所有用户的回收站

### 3.2 数据库设计（oa_document库）

```sql
-- 文件夹
CREATE TABLE doc_folder (
    folder_id     BIGINT       NOT NULL AUTO_INCREMENT,
    folder_name   VARCHAR(100) NOT NULL COMMENT '文件夹名称',
    parent_id     BIGINT       DEFAULT 0 COMMENT '父文件夹ID',
    ancestors     VARCHAR(255) DEFAULT '' COMMENT '祖级路径',
    library_type  CHAR(1)      DEFAULT '0' COMMENT '库类型(0公共 1部门 2项目)',
    library_id    BIGINT       DEFAULT 0 COMMENT '关联ID(部门ID/项目ID)',
    order_num     INT          DEFAULT 0,
    status        CHAR(1)      DEFAULT '0',
    create_by     VARCHAR(64)  DEFAULT '',
    create_time   DATETIME     DEFAULT NULL,
    update_by     VARCHAR(64)  DEFAULT '',
    update_time   DATETIME     DEFAULT NULL,
    del_flag      CHAR(1)      DEFAULT '0',
    tenant_id     BIGINT       DEFAULT 0,
    PRIMARY KEY (folder_id)
) ENGINE=InnoDB COMMENT='文档文件夹表';

-- 文件
CREATE TABLE doc_file (
    file_id       BIGINT       NOT NULL AUTO_INCREMENT,
    folder_id     BIGINT       NOT NULL COMMENT '文件夹ID',
    file_name     VARCHAR(255) NOT NULL COMMENT '文件名',
    file_type     VARCHAR(20)  NOT NULL COMMENT '文件类型(doc/xls/ppt/pdf/img/video/audio/other)',
    file_size     BIGINT       DEFAULT 0 COMMENT '文件大小(字节)',
    file_path     VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_url      VARCHAR(500) DEFAULT '' COMMENT '访问URL',
    file_md5      VARCHAR(64)  DEFAULT '' COMMENT 'MD5(秒传判断)',
    version       INT          DEFAULT 1 COMMENT '当前版本号',
    description   VARCHAR(500) DEFAULT '' COMMENT '描述',
    tags          VARCHAR(500) DEFAULT '' COMMENT '标签(逗号分隔)',
    view_count    INT          DEFAULT 0 COMMENT '浏览次数',
    download_count INT         DEFAULT 0 COMMENT '下载次数',
    collect_count INT          DEFAULT 0 COMMENT '收藏次数',
    status        CHAR(1)      DEFAULT '0' COMMENT '状态(0正常 1审核中 2已拒绝)',
    create_by     VARCHAR(64)  DEFAULT '',
    create_time   DATETIME     DEFAULT NULL,
    update_by     VARCHAR(64)  DEFAULT '',
    update_time   DATETIME     DEFAULT NULL,
    del_flag      CHAR(1)      DEFAULT '0',
    tenant_id     BIGINT       DEFAULT 0,
    PRIMARY KEY (file_id),
    KEY idx_folder (folder_id),
    KEY idx_md5 (file_md5)
) ENGINE=InnoDB COMMENT='文件表';

-- 文件版本
CREATE TABLE doc_file_version (
    version_id    BIGINT       NOT NULL AUTO_INCREMENT,
    file_id       BIGINT       NOT NULL COMMENT '文件ID',
    version_num   INT          NOT NULL COMMENT '版本号',
    file_path     VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size     BIGINT       DEFAULT 0,
    file_md5      VARCHAR(64)  DEFAULT '',
    change_log    VARCHAR(500) DEFAULT '' COMMENT '变更说明',
    create_by     VARCHAR(64)  DEFAULT '',
    create_time   DATETIME     DEFAULT NULL,
    PRIMARY KEY (version_id),
    KEY idx_file (file_id)
) ENGINE=InnoDB COMMENT='文件版本表';

-- 文件收藏
CREATE TABLE doc_collect (
    collect_id    BIGINT       NOT NULL AUTO_INCREMENT,
    file_id       BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    create_time   DATETIME     DEFAULT NULL,
    PRIMARY KEY (collect_id),
    UNIQUE KEY uk_user_file (user_id, file_id)
) ENGINE=InnoDB COMMENT='文件收藏表';

-- 文件权限
CREATE TABLE doc_permission (
    permission_id BIGINT       NOT NULL AUTO_INCREMENT,
    target_type   CHAR(1)      NOT NULL COMMENT '目标类型(0文件夹 1文件)',
    target_id     BIGINT       NOT NULL COMMENT '目标ID',
    subject_type  CHAR(1)      NOT NULL COMMENT '主体类型(0用户 1角色 2部门)',
    subject_id    BIGINT       NOT NULL COMMENT '主体ID',
    permission    CHAR(1)      NOT NULL COMMENT '权限(1查看 2编辑 3管理)',
    create_by     VARCHAR(64)  DEFAULT '',
    create_time   DATETIME     DEFAULT NULL,
    PRIMARY KEY (permission_id)
) ENGINE=InnoDB COMMENT='文件权限表';
```

## 4. 沟通与协作

### 4.1 待办中心

OA系统中使用频率最高的页面，聚合所有需要用户处理的事项。

**待办聚合：**
| 来源 | 待办类型 | 操作 |
|------|---------|------|
| 流程审批 | 审批待办 | 同意/拒绝/转办 |
| 流程审批 | 抄送知会 | 仅查看 |
| 通知公告 | 未读公告 | 查看/标记已读 |
| 会议管理 | 会议邀请 | 接受/拒绝 |
| 日程管理 | 日程提醒 | 查看/推迟 |
| 系统通知 | 密码过期提醒 | 修改密码 |
| 系统通知 | 账号异常告警 | 查看 |

**待办中心功能：**
- 按类型筛选（全部/审批/公告/会议/日程/系统）
- 按紧急程度排序
- 一键批量处理（如批量同意低风险审批）
- 待办数量徽章（Navbar右上角实时显示）
- 待办推送（站内信 + 可选短信/邮件）

### 4.2 企业IM

基于WebSocket的轻量级企业即时通讯：

**功能：**
- 单聊：文字、图片、文件、表情
- 群聊：创建群组、邀请/移除成员、@提醒、群公告
- 消息与业务关联：在聊天中发送审批单链接，点击可直接打开审批
- 消息已读/未读状态
- 消息搜索（按联系人、关键词）
- 离线消息推送（通过站内信/短信通知）

**数据存储：** 消息记录永久保留，支持按会话查看历史消息。

### 4.3 会议管理

**会议全流程：**
1. **会前：** 发起会议 → 选择时间/会议室/参会人 → 检测时间冲突 → 发送邀请通知
2. **会中：** 会议签到 → 会议纪要实时记录 → 附件共享
3. **会后：** 纪要整理发布 → 待办任务拆解（可关联任务跟踪）→ 纪要归档

**会议室管理：**
- 会议室资源维护（位置、容量、设备）
- 会议室预约（日历视图，冲突检测）
- 预约审批（可选）
- 使用统计

### 4.4 日程管理

- 个人日程：创建/编辑/删除日程，设置提醒时间
- 共享日程：设置共享范围（指定人/部门）
- 团队日历：查看团队成员日程安排，便于协调会议时间
- 日程视图：日/周/月视图切换
- 日程提醒：提前N分钟提醒（站内信/短信）

### 4.5 通讯录

- 按组织架构树展示（与部门管理同步）
- 支持拼音首字母快速检索
- 显示：姓名、部门、岗位、手机、邮箱、工位
- vCard导出
- 离职人员灰色标记

### 4.6 数据库设计

```sql
-- 待办事项（存oa_system库）
-- 复用已有sys_notice表扩展或独立建表
CREATE TABLE biz_todo (
    todo_id        BIGINT       NOT NULL AUTO_INCREMENT,
    todo_type      VARCHAR(20)  NOT NULL COMMENT '类型(approval/notice/meeting/schedule/system)',
    title          VARCHAR(200) NOT NULL COMMENT '标题',
    content        VARCHAR(500) DEFAULT '' COMMENT '内容摘要',
    business_id    BIGINT       DEFAULT NULL COMMENT '关联业务ID',
    business_url   VARCHAR(255) DEFAULT '' COMMENT '业务跳转URL',
    sender_id      BIGINT       DEFAULT NULL COMMENT '发送人ID',
    sender_name    VARCHAR(30)  DEFAULT '' COMMENT '发送人姓名',
    receiver_id    BIGINT       NOT NULL COMMENT '接收人ID',
    urgent_level   CHAR(1)      DEFAULT '0' COMMENT '紧急程度(0普通 1重要 2紧急)',
    read_flag      CHAR(1)      DEFAULT '0' COMMENT '已读标志',
    read_time      DATETIME     DEFAULT NULL,
    handle_flag    CHAR(1)      DEFAULT '0' COMMENT '处理标志',
    handle_time    DATETIME     DEFAULT NULL,
    expire_time    DATETIME     DEFAULT NULL COMMENT '过期时间',
    create_time    DATETIME     DEFAULT NULL,
    tenant_id      BIGINT       DEFAULT 0,
    PRIMARY KEY (todo_id),
    KEY idx_receiver (receiver_id, read_flag, handle_flag)
) ENGINE=InnoDB COMMENT='待办事项表';

-- IM会话
CREATE TABLE im_conversation (
    conversation_id BIGINT      NOT NULL AUTO_INCREMENT,
    conversation_type CHAR(1)   NOT NULL COMMENT '类型(0单聊 1群聊)',
    conversation_name VARCHAR(50) DEFAULT '' COMMENT '会话名称(群聊时)',
    owner_id        BIGINT      NOT NULL COMMENT '创建者ID',
    last_message    VARCHAR(500) DEFAULT '' COMMENT '最后一条消息',
    last_message_time DATETIME   DEFAULT NULL,
    status          CHAR(1)      DEFAULT '0',
    create_time     DATETIME     DEFAULT NULL,
    tenant_id       BIGINT       DEFAULT 0,
    PRIMARY KEY (conversation_id)
) ENGINE=InnoDB COMMENT='IM会话表';

-- IM会话成员
CREATE TABLE im_conversation_member (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    nickname        VARCHAR(30) DEFAULT '' COMMENT '群昵称',
    role            CHAR(1)     DEFAULT '0' COMMENT '角色(0成员 1管理员 2群主)',
    last_read_msg_id BIGINT     DEFAULT 0 COMMENT '最后已读消息ID',
    mute_flag       CHAR(1)     DEFAULT '0' COMMENT '免打扰',
    join_time       DATETIME    DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_conv_user (conversation_id, user_id)
) ENGINE=InnoDB COMMENT='IM会话成员表';

-- IM消息
CREATE TABLE im_message (
    message_id      BIGINT       NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT       NOT NULL,
    sender_id       BIGINT       NOT NULL,
    sender_name     VARCHAR(30)  NOT NULL,
    msg_type        CHAR(1)      NOT NULL COMMENT '消息类型(0文字 1图片 2文件 3链接 4系统)',
    content         TEXT         NOT NULL COMMENT '消息内容',
    extra           TEXT         DEFAULT NULL COMMENT '扩展数据JSON',
    send_time       DATETIME     NOT NULL,
    recalled_flag   CHAR(1)      DEFAULT '0' COMMENT '撤回标志',
    PRIMARY KEY (message_id),
    KEY idx_conv_time (conversation_id, send_time)
) ENGINE=InnoDB COMMENT='IM消息表';

-- 会议室
CREATE TABLE meeting_room (
    room_id         BIGINT       NOT NULL AUTO_INCREMENT,
    room_name       VARCHAR(50)  NOT NULL COMMENT '会议室名称',
    location        VARCHAR(200) DEFAULT '' COMMENT '位置',
    capacity        INT          DEFAULT 0 COMMENT '容量',
    facilities      VARCHAR(500) DEFAULT '' COMMENT '设备(逗号分隔)',
    status          CHAR(1)      DEFAULT '0',
    create_by       VARCHAR(64)  DEFAULT '',
    create_time     DATETIME     DEFAULT NULL,
    update_by       VARCHAR(64)  DEFAULT '',
    update_time     DATETIME     DEFAULT NULL,
    del_flag        CHAR(1)      DEFAULT '0',
    tenant_id       BIGINT       DEFAULT 0,
    PRIMARY KEY (room_id)
) ENGINE=InnoDB COMMENT='会议室表';

-- 会议
CREATE TABLE meeting (
    meeting_id      BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200) NOT NULL COMMENT '会议主题',
    room_id         BIGINT       DEFAULT NULL COMMENT '会议室ID',
    meeting_date    DATE         NOT NULL COMMENT '会议日期',
    start_time      TIME         NOT NULL COMMENT '开始时间',
    end_time        TIME         NOT NULL COMMENT '结束时间',
    organizer_id    BIGINT       NOT NULL COMMENT '组织者ID',
    organizer_name  VARCHAR(30)  NOT NULL,
    description     TEXT         DEFAULT NULL COMMENT '会议描述',
    meeting_type    CHAR(1)      DEFAULT '0' COMMENT '类型(0线下 1线上 2混合)',
    meeting_url     VARCHAR(500) DEFAULT '' COMMENT '线上会议链接',
    minutes         TEXT         DEFAULT NULL COMMENT '会议纪要',
    status          CHAR(1)      DEFAULT '0' COMMENT '状态(0待开始 1进行中 2已结束 3已取消)',
    create_by       VARCHAR(64)  DEFAULT '',
    create_time     DATETIME     DEFAULT NULL,
    update_by       VARCHAR(64)  DEFAULT '',
    update_time     DATETIME     DEFAULT NULL,
    del_flag        CHAR(1)      DEFAULT '0',
    tenant_id       BIGINT       DEFAULT 0,
    PRIMARY KEY (meeting_id)
) ENGINE=InnoDB COMMENT='会议表';

-- 会议参与人
CREATE TABLE meeting_participant (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    meeting_id      BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    user_name       VARCHAR(30) NOT NULL,
    response_status CHAR(1)     DEFAULT '0' COMMENT '响应(0待确认 1接受 2拒绝)',
    attend_status   CHAR(1)     DEFAULT '0' COMMENT '签到(0未签到 1已签到)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meeting_user (meeting_id, user_id)
) ENGINE=InnoDB COMMENT='会议参与人表';

-- 日程
CREATE TABLE biz_schedule (
    schedule_id     BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200) NOT NULL COMMENT '标题',
    content         TEXT         DEFAULT NULL COMMENT '内容',
    start_time      DATETIME     NOT NULL COMMENT '开始时间',
    end_time        DATETIME     NOT NULL COMMENT '结束时间',
    is_all_day      CHAR(1)      DEFAULT '0' COMMENT '是否全天',
    location        VARCHAR(200) DEFAULT '' COMMENT '地点',
    reminder_minutes INT         DEFAULT 15 COMMENT '提前提醒(分钟, 0不提醒)',
    share_scope     VARCHAR(500) DEFAULT '' COMMENT '共享范围JSON',
    owner_id        BIGINT       NOT NULL COMMENT '所属人ID',
    source_type     VARCHAR(20)  DEFAULT 'manual' COMMENT '来源(manual/meeting/task)',
    source_id       BIGINT       DEFAULT NULL COMMENT '来源ID',
    create_time     DATETIME     DEFAULT NULL,
    update_time     DATETIME     DEFAULT NULL,
    del_flag        CHAR(1)      DEFAULT '0',
    tenant_id       BIGINT       DEFAULT 0,
    PRIMARY KEY (schedule_id),
    KEY idx_owner_time (owner_id, start_time)
) ENGINE=InnoDB COMMENT='日程表';
```

## 5. 前端新增页面

### 5.1 流程审批页面

```
views/workflow/
├── category/index.vue         # 流程分类管理
├── form/
│   ├── index.vue              # 表单列表
│   └── designer.vue           # 表单设计器
├── definition/
│   ├── index.vue              # 流程定义列表
│   └── designer.vue           # 流程设计器（核心页面）
├── process/
│   ├── start.vue              # 发起流程（选择流程类型）
│   ├── detail.vue             # 流程详情（表单+流程图+审批历史）
│   ├── my.vue                 # 我发起的
│   ├── todo.vue               # 我的待办
│   ├── done.vue               # 我的已办
│   └── cc.vue                 # 抄送我的
├── delegation/index.vue       # 委托设置
└── stats/index.vue            # 流程统计

components/
├── FlowDesigner/              # 流程设计器组件
│   ├── index.vue
│   ├── nodes/                 # 各节点组件
│   └── panels/                # 属性配置面板
├── FormDesigner/              # 表单设计器组件
│   ├── index.vue
│   └── widgets/               # 各表单控件
├── FlowChart/                 # 流程图展示组件（带状态高亮）
└── ApprovalAction/            # 审批操作组件
```

### 5.2 文档管理页面

```
views/document/
├── index.vue                  # 文档库主页（左侧文件夹树 + 右侧文件列表）
├── preview.vue                # 文件在线预览
├── editor.vue                 # 在线编辑
├── search.vue                 # 全文检索
├── collect.vue                # 我的收藏
├── recycle.vue                # 回收站
└── admin/
    └── permission.vue         # 权限管理
```

### 5.3 沟通与协作页面

```
views/collaboration/
├── todo/index.vue             # 待办中心
├── im/
│   ├── index.vue              # IM主页面
│   └── components/
│       ├── ChatList.vue
│       ├── ChatWindow.vue
│       └── MessageItem.vue
├── meeting/
│   ├── index.vue              # 会议管理
│   ├── room.vue               # 会议室管理
│   └── minutes.vue            # 纪要管理
├── schedule/index.vue         # 日程管理（日/周/月视图）
└── contacts/index.vue         # 通讯录
```

## 6. 技术要点

### 6.1 流程设计器技术方案

前端流程设计器基于Canvas或SVG实现：
- 使用Vue Flow（基于react-flow的Vue 3版本）或AntV X6图编辑引擎
- 节点拖拽、连线、属性面板
- 导出为BPMN JSON格式存储
- 后端解析JSON执行流程逻辑

后端不依赖Flowable/Camunda等重型引擎，自行实现轻量级流程引擎：
- 解析流程定义JSON，构建内存中的流程图结构
- 根据流程图和表单数据执行节点流转
- 自行实现或签/会签/条件分支等审批逻辑
- 优势：完全可控、轻量、无外部依赖

### 6.2 文件在线预览方案

| 方案 | 说明 |
|------|------|
| kkFileView | 开源文件预览服务，支持Office/PDF/图片/视频 |
| OnlyOffice | 开源在线编辑+预览，需部署Document Server |
| LibreOffice Headless | 服务端转换PDF后前端预览 |

推荐：预览用kkFileView，编辑用OnlyOffice。

### 6.3 WebSocket IM方案

- 网关层维护WebSocket连接
- Redis Pub/Sub实现多实例消息同步
- 消息持久化到MySQL
- 前端使用原生WebSocket + 自动重连

## 7. 成功标准

- 流程设计器能拖拽创建审批流程，保存并发布
- 用户能发起流程，审批人能审批（同意/拒绝/转办/加签）
- 流程流转图能实时高亮显示当前节点
- 文件能上传下载、在线预览Office/PDF
- IM能收发消息，消息不丢失
- 待办中心聚合所有待办事项，实时更新
- 会议室预约能检测时间冲突
