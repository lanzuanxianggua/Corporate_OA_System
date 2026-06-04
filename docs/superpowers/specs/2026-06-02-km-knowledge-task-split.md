# 知识库 oa-knowledge 模块重构实施与任务拆分

> 日期: 2026-06-03
> 实施范围: 文档与知识管理（词条/版本/标签/检索/推荐/收藏）
> 主设计文档: `docs/superpowers/specs/2026-06-02-oa-system-redesign.md` 第 3.2 节
> 参考试点: `docs/superpowers/specs/2026-06-02-hr-leave-pilot-task-split.md`

---

## 1. 模块目标

通过 11 个波次完成 `oa-knowledge` 知识库模块的完整落地，对齐重构文档 3.2 节。

完成后应具备：

1. `km_*` 共 6 张核心表结构与种子数据。
2. `oa-knowledge` 模块内的词条 CRUD、版本控制、标签管理。
3. Elasticsearch 8.x 索引 `km_document` 与增量同步机制。
4. 协同过滤风格的推荐算法（基于浏览/收藏/标签相似度）。
5. 收藏与评分基础能力（用户-词条-评分三元组）。
6. `/api/knowledge/*` REST API 全部就绪，含 Knife4j 注解与权限码。
7. 后端单元测试、Controller 集成测试、Web/Mobile 端页面打通。
8. 演示数据、监控埋点与验收清单齐备。

---

## 2. 边界

### 2.1 本次重构包含

| 区域 | 内容 |
|------|------|
| 数据库 | `km_entry`、`km_version`、`km_tag`、`km_relation`、`km_category`、`km_read_record` 共 6 张表 |
| ES 索引 | `km_document` 索引 + IK 分词配置 + 同步策略 |
| 后端 | `oa-knowledge` 模块（entity/dto/vo/mapper/service/controller/integration） |
| API | `/api/knowledge/*` 全部接口（词条/版本/标签/检索/推荐/收藏/分类） |
| 推荐 | 基于浏览记录 + 标签 + 收藏相似度的协同过滤算法（内存版，可后续迁移到 Spark/Flink） |
| 评分 | 简单显式评分（1-5 星）+ 隐式评分（浏览 + 收藏 + 标签命中权重） |
| 测试 | Service 单测、Controller 集成测试、ES 同步集成测试（使用 ES 容器） |
| 监控 | 检索耗时、推荐耗时、同步失败次数埋点 |

### 2.2 本次重构不包含

| 不包含 | 原因 |
|--------|------|
| 复杂知识图谱关系（实体-关系-属性三元组） | 范围过大，留给 AI 中台后续阶段 |
| 文件防泄密水印 | 放 Web 共享组件 `Watermark` 实现，不在知识库后端 |
| 文档在线协同编辑（OT/CRDT） | 协同用版本控制代替，避免引入 Yjs/ShareDB |
| 移动端富文本编辑 | 移动端只读 + 评论，不实现编辑器 |
| ES 集群分片调优、跨集群复制 | 部署阶段处理，业务模块不涉及 |
| 国际化多语言标题/正文 | 预留 `locale` 字段，本期只存中文 |
| AI 自动摘要/打标 | 后续 AI 中台接入，本期只接 ES + 协同过滤 |

---

## 3. 任务波次

### Wave 1: 契约与基线

#### T1 数据库与 API 契约

| 字段 | 内容 |
|------|------|
| 目标 | 定义知识库闭环的数据表、接口、权限码、DTO/VO、ES 索引 schema |
| 路径 | `code/backend/sql/km_knowledge_contract.sql`、`docs/superpowers/specs/2026-06-02-oa-system-redesign.md` 第 3.2 节 |
| 输入 | 重构文档 3.2 节、旧 `oa_document`、`oa_notice` 知识相关实现 |
| 输出 | DDL/seed 草案、ES 索引 schema、API 契约表、权限码清单、字段枚举、推荐算法伪代码 |
| 禁止修改 | 不实现 Service/Controller 业务逻辑，不修改正式 baseline SQL |
| 验收 | 文档列出 6 张表字段、索引、ES mapping、11 类 API、权限码、验收命令 |

#### T2 旧实现影响分析

| 字段 | 内容 |
|------|------|
| 目标 | 查清旧文档/通知中可能涉及知识库功能的实体、Mapper、Service、Controller、前端/移动端依赖 |
| 路径 | `code/backend/oa-model`、`oa-mapper`、`oa-service`、`oa-web`、`code/frontend/src/api/document.ts`、`code/mobile/src/api/document.ts` |
| 输出 | 旧入口清单、迁移保留/替换/下线建议 |
| 禁止修改 | 不删除旧代码 |
| 验收 | 影响分析文档或本文件追加清单 |

### Wave 2: 数据模型与 ES 集成

#### T3 实体与 Mapper

| 字段 | 内容 |
|------|------|
| 目标 | 在 `oa-knowledge` 中建立 `km_entry`/`km_version`/`km_tag`/`km_relation`/`km_category`/`km_read_record` 实体和 Mapper |
| 路径 | `code/backend/oa-knowledge`、必要时 `code/backend/sql` |
| 输出 | Entity、Enum、Mapper、基础查询方法、Mapper 测试或集成测试 |
| 禁止修改 | 不修改前端、移动端、不重写 workflow 核心 |
| 验收 | `cd code/backend && mvn -pl oa-knowledge -am test` |

#### T4 Elasticsearch 索引与同步

| 字段 | 内容 |
|------|------|
| 目标 | 建立 `km_document` 索引（IK 分词）、`EsSyncService` 同步策略、`KmIndexEvent` 事件发布 |
| 路径 | `code/backend/oa-knowledge/src/main/java/cn/oa/knowledge/integration/es` |
| 输出 | EsClient 配置类、IndexMapping JSON、同步服务、事件订阅、集成测试（Testcontainers ES） |
| 禁止修改 | 不修改 oa-integration 中已有通用 ES 配置（除非缺失基础封装） |
| 验收 | 启动后能创建 `km_document` 索引，词条创建/更新/删除能增量同步到 ES |

### Wave 3: Service 与业务规则

#### T5 Service 与业务规则

| 字段 | 内容 |
|------|------|
| 目标 | 实现词条创建/发布/归档/版本上传/标签管理/浏览计数等业务 Service |
| 路径 | `code/backend/oa-knowledge` |
| 输出 | Service 接口/实现、DTO/VO、单元测试、ES 同步触发、推荐打分回写 |
| 禁止修改 | 不直接操作 ES 内部 API（统一走 EsSyncService） |
| 验收 | `cd code/backend && mvn -pl oa-knowledge -am test` |

#### T6 推荐算法 Service

| 字段 | 内容 |
|------|------|
| 目标 | 实现基于协同过滤的推荐：浏览记录相似度 + 标签命中权重 + 收藏加权 |
| 路径 | `code/backend/oa-knowledge/src/main/java/cn/oa/knowledge/recommend` |
| 输出 | `RecommendService` 接口、Item-CF + Tag-CF 混合实现、推荐打分结果可解释（命中原因） |
| 禁止修改 | 不引入 Spark/Flink/外部推荐服务；纯内存 + Redis 缓存 |
| 验收 | 单元测试覆盖冷启动、热门推荐、个性化推荐三种场景 |

### Wave 4: REST API

#### T7 Controller 层（词条/版本/标签/分类/浏览）

| 字段 | 内容 |
|------|------|
| 目标 | 暴露词条/版本/标签/分类/浏览相关 REST API，对齐 `/api/knowledge/*` 契约 |
| 路径 | `code/backend/oa-knowledge/src/main/java/cn/oa/knowledge/controller` |
| 输出 | Controller、OpenAPI 注解、`@RequirePermission` 注解、Controller 测试 |
| 禁止修改 | 不复制旧 Controller 大段逻辑 |
| 验收 | `cd code/backend && mvn -pl oa-knowledge,oa-web -am test` |

#### T8 Controller 层（检索/推荐/收藏/评分）

| 字段 | 内容 |
|------|------|
| 目标 | 暴露 ES 全文检索、推荐列表、收藏、显式评分 REST API |
| 路径 | `code/backend/oa-knowledge/src/main/java/cn/oa/knowledge/controller` |
| 输出 | SearchController、RecommendController、FavoriteController、RatingController + 集成测试 |
| 禁止修改 | 不修改工作流任务 API |
| 验收 | `cd code/backend && mvn -pl oa-knowledge,oa-web -am test` |

### Wave 5: 测试与演示

#### T9 接口测试与权限

| 字段 | 内容 |
|------|------|
| 目标 | 完成 OpenAPI 注解补齐、Knife4j 分组、权限码矩阵验证、接口契约单测 |
| 路径 | `code/backend/oa-knowledge/src/test`、`code/backend/oa-web/src/test` |
| 输出 | 接口契约测试、权限注解验证、Knife4j 文档分组 |
| 禁止修改 | 不改 Service 业务规则 |
| 验收 | `cd code/backend && mvn -pl oa-knowledge -am test` |

#### T10 E2E 与演示数据

| 字段 | 内容 |
|------|------|
| 目标 | 端到端验证：词条创建 → ES 同步 → 检索 → 推荐 → 收藏 → 评分；同时提供演示数据 seed |
| 路径 | `code/backend/sql/seed/004_seed_knowledge.sql`、`code/backend/oa-knowledge/src/test/java/cn/oa/knowledge/e2e` |
| 输出 | E2E 测试（Testcontainers MySQL + ES）、演示数据 seed 脚本（部门/员工/分类/词条/标签） |
| 禁止修改 | 不删改正式 baseline，只新增 seed 脚本 |
| 验收 | `cd code/backend && mvn -pl oa-knowledge -P e2e test` |

### Wave 6: 监控与验收

#### T11 监控埋点与验收清单

| 字段 | 内容 |
|------|------|
| 目标 | 接入 micrometer 指标（检索耗时、推荐耗时、同步失败次数）；输出最终验收报告 |
| 路径 | `code/backend/oa-knowledge`、`docs/superpowers/specs/2026-06-02-km-knowledge-task-split.md` |
| 输出 | Micrometer Counter/Timer、OperationLog 切面、最终验收报告（旧入口下线清单） |
| 禁止修改 | 不修改 micrometer 全局配置（统一在 oa-web） |
| 验收 | `/actuator/metrics/km.search.duration` 等指标可见；旧入口下线清单完整 |

---

## 4. 推荐执行顺序

```
Wave 1: T1 + T2
Wave 2: T3 -> T4
Wave 3: T5 -> T6
Wave 4: T7 -> T8
Wave 5: T9 -> T10
Wave 6: T11
```

T1/T2 完成前不得开始代码实现。T4 ES 同步必须先于 T5 Service 中的发布/归档动作，避免发布后未同步。T6 推荐算法可在 T5 之后并行，但 T8 推荐 Controller 必须等 T6 完成。

---

## 5. 完整 DDL 草案

### 5.1 知识词条表 `km_entry`

```sql
CREATE TABLE `km_entry` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT          COMMENT '词条ID',
  `title`            VARCHAR(200)  NOT NULL                          COMMENT '标题',
  `summary`          VARCHAR(500)  DEFAULT NULL                      COMMENT '摘要',
  `current_version`  INT           NOT NULL DEFAULT 1                COMMENT '当前版本号',
  `status`           VARCHAR(16)   NOT NULL DEFAULT 'DRAFT'          COMMENT '状态(DRAFT/PUBLISHED/ARCHIVED)',
  `dept_id`          BIGINT        DEFAULT NULL                      COMMENT '归属部门ID',
  `category_id`      BIGINT        DEFAULT NULL                      COMMENT '分类ID',
  `security_level`   VARCHAR(16)   NOT NULL DEFAULT 'PUBLIC'         COMMENT '密级(PUBLIC/INTERNAL/SECRET)',
  `view_count`       INT           NOT NULL DEFAULT 0                COMMENT '浏览次数',
  `download_count`   INT           NOT NULL DEFAULT 0                COMMENT '下载次数',
  `favorite_count`   INT           NOT NULL DEFAULT 0                COMMENT '收藏次数',
  `rating_avg`       DECIMAL(3,2)  NOT NULL DEFAULT 0.00             COMMENT '平均评分(0-5)',
  `rating_count`     INT           NOT NULL DEFAULT 0                COMMENT '评分次数',
  `uploader_id`      BIGINT        NOT NULL                          COMMENT '创建人ID',
  `publisher_id`     BIGINT        DEFAULT NULL                      COMMENT '发布人ID',
  `publish_time`     DATETIME      DEFAULT NULL                      COMMENT '发布时间',
  `del_flag`         CHAR(1)       NOT NULL DEFAULT '0',
  `create_by`        VARCHAR(64)   DEFAULT NULL,
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`        VARCHAR(64)   DEFAULT NULL,
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_km_entry_dept`     (`dept_id`),
  KEY `idx_km_entry_category` (`category_id`),
  KEY `idx_km_entry_status`   (`status`),
  KEY `idx_km_entry_uploader` (`uploader_id`),
  KEY `idx_km_entry_publish`  (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识词条表';
```

### 5.2 知识版本表 `km_version`

```sql
CREATE TABLE `km_version` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT        COMMENT '版本ID',
  `entry_id`    BIGINT        NOT NULL                        COMMENT '词条ID',
  `version_no`  INT           NOT NULL                        COMMENT '版本号',
  `file_path`   VARCHAR(512)  NOT NULL                        COMMENT '文件路径',
  `file_name`   VARCHAR(255)  DEFAULT NULL                    COMMENT '原始文件名',
  `file_size`   BIGINT        NOT NULL DEFAULT 0              COMMENT '文件大小(字节)',
  `file_type`   VARCHAR(32)   DEFAULT NULL                    COMMENT '文件类型(pdf/docx/xlsx/pptx/md/html)',
  `file_hash`   VARCHAR(64)   DEFAULT NULL                    COMMENT '文件SHA256',
  `content_text` LONGTEXT     DEFAULT NULL                    COMMENT '提取后的纯文本(用于ES索引)',
  `uploader_id` BIGINT        NOT NULL                        COMMENT '上传人ID',
  `upload_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `comment`     VARCHAR(500)  DEFAULT NULL                    COMMENT '版本说明',
  `is_current`  TINYINT       NOT NULL DEFAULT 0              COMMENT '是否当前版本(0否1是)',
  `del_flag`    CHAR(1)       NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_km_version_entry_no` (`entry_id`, `version_no`),
  KEY `idx_km_version_entry`     (`entry_id`),
  KEY `idx_km_version_uploader`  (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识版本表';
```

### 5.3 知识分类表 `km_category`

```sql
CREATE TABLE `km_category` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT         COMMENT '分类ID',
  `parent_id`  BIGINT        DEFAULT NULL                     COMMENT '父分类ID(0为根)',
  `name`       VARCHAR(64)   NOT NULL                         COMMENT '分类名称',
  `code`       VARCHAR(64)   NOT NULL                         COMMENT '分类编码(英文唯一)',
  `icon`       VARCHAR(255)  DEFAULT NULL                     COMMENT '分类图标',
  `sort_order` INT           NOT NULL DEFAULT 0               COMMENT '排序号(小优先)',
  `dept_id`    BIGINT        DEFAULT NULL                     COMMENT '归属部门(NULL=公共)',
  `status`     VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE'        COMMENT '状态(ACTIVE/DISABLED)',
  `description` VARCHAR(500)  DEFAULT NULL                     COMMENT '分类描述',
  `del_flag`   CHAR(1)       NOT NULL DEFAULT '0',
  `create_by`  VARCHAR(64)   DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by`  VARCHAR(64)   DEFAULT NULL,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_km_category_code` (`code`),
  KEY `idx_km_category_parent` (`parent_id`),
  KEY `idx_km_category_dept`   (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识分类表';
```

### 5.4 知识标签表 `km_tag`

```sql
CREATE TABLE `km_tag` (
  `id`        BIGINT        NOT NULL AUTO_INCREMENT          COMMENT '标签ID',
  `entry_id`  BIGINT        NOT NULL                          COMMENT '词条ID',
  `tag_name`  VARCHAR(64)   NOT NULL                          COMMENT '标签名',
  `tag_type`  VARCHAR(16)   NOT NULL DEFAULT 'CUSTOM'         COMMENT '标签类型(CUSTOM/SYSTEM/CATEGORY)',
  `use_count` INT           NOT NULL DEFAULT 0                COMMENT '使用次数(冗余字段)',
  `create_by` VARCHAR(64)   DEFAULT NULL,
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_km_tag_entry_name` (`entry_id`, `tag_name`),
  KEY `idx_km_tag_entry` (`entry_id`),
  KEY `idx_km_tag_name`  (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识标签表';
```

### 5.5 知识关联表 `km_relation`

```sql
CREATE TABLE `km_relation` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT                COMMENT '关联ID',
  `entry_id`         BIGINT       NOT NULL                                COMMENT '源词条ID',
  `related_entry_id` BIGINT       NOT NULL                                COMMENT '目标词条ID',
  `relation_type`    VARCHAR(32)  NOT NULL DEFAULT 'REFERENCE'           COMMENT '关联类型(REFERENCE/SIMILAR/PARENT/SEE_ALSO)',
  `score`            DECIMAL(5,4) DEFAULT NULL                            COMMENT '关联得分(0.0000-1.0000)',
  `algorithm`        VARCHAR(32)  DEFAULT 'MANUAL'                         COMMENT '生成方式(MANUAL/TAG_CF/ITEM_CF)',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_km_relation_pair` (`entry_id`, `related_entry_id`, `relation_type`),
  KEY `idx_km_relation_entry`   (`entry_id`),
  KEY `idx_km_relation_related` (`related_entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识关联表';
```

### 5.6 知识浏览/收藏/评分记录表 `km_read_record`

```sql
CREATE TABLE `km_read_record` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT       COMMENT '记录ID',
  `entry_id`   BIGINT        NOT NULL                       COMMENT '词条ID',
  `emp_id`     BIGINT        NOT NULL                       COMMENT '员工ID',
  `action_type` VARCHAR(16)   NOT NULL                       COMMENT '动作(VIEW/DOWNLOAD/FAVORITE/UNFAVORITE/RATE)',
  `rating`     TINYINT       DEFAULT NULL                   COMMENT '评分(1-5, 仅RATE动作有值)',
  `weight`     DECIMAL(5,2)  NOT NULL DEFAULT 1.00          COMMENT '权重(浏览=1.0 收藏=3.0 评分=4.0)',
  `source`     VARCHAR(32)   DEFAULT 'WEB'                  COMMENT '来源(WEB/MOBILE/ES)',
  `client_ip`  VARCHAR(64)   DEFAULT NULL                   COMMENT '客户端IP',
  `user_agent` VARCHAR(255)  DEFAULT NULL                   COMMENT 'UA',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_km_record_entry`  (`entry_id`),
  KEY `idx_km_record_emp`    (`emp_id`),
  KEY `idx_km_record_action` (`action_type`),
  KEY `idx_km_record_time`   (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识浏览/收藏/评分记录表';
```

### 5.7 ES 索引 `km_document`

```json
PUT /km_document
{
  "settings": {
    "number_of_shards": 2,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_smart_analyzer":  { "type": "custom", "tokenizer": "ik_smart" },
        "ik_max_word_analyzer": { "type": "custom", "tokenizer": "ik_max_word" }
      }
    }
  },
  "mappings": {
    "properties": {
      "id":              { "type": "long" },
      "title":           { "type": "text",  "analyzer": "ik_max_word_analyzer", "search_analyzer": "ik_smart_analyzer",
                           "fields": { "keyword": { "type": "keyword", "ignore_above": 256 } } },
      "summary":         { "type": "text",  "analyzer": "ik_max_word_analyzer", "search_analyzer": "ik_smart_analyzer" },
      "content":         { "type": "text",  "analyzer": "ik_max_word_analyzer", "search_analyzer": "ik_smart_analyzer" },
      "tags":            { "type": "keyword" },
      "category_id":     { "type": "long" },
      "category_name":   { "type": "keyword" },
      "dept_id":         { "type": "long" },
      "security_level":  { "type": "keyword" },
      "status":          { "type": "keyword" },
      "file_type":       { "type": "keyword" },
      "view_count":      { "type": "integer" },
      "download_count":  { "type": "integer" },
      "favorite_count":  { "type": "integer" },
      "rating_avg":      { "type": "float" },
      "uploader_id":     { "type": "long" },
      "publisher_id":    { "type": "long" },
      "create_time":     { "type": "date" },
      "update_time":     { "type": "date" },
      "publish_time":    { "type": "date" }
    }
  }
}
```

### 5.8 索引与 EXPLAIN 验收

| 查询场景 | 索引 | 验收 |
|----------|------|------|
| 我的词条列表 | `idx_km_entry_uploader(uploader_id,status,create_time)` | EXPLAIN 命中，不出现全表扫描 |
| 部门词条列表 | `idx_km_entry_dept(dept_id,status,publish_time)` | 按部门过滤时命中 |
| 分类下词条 | `idx_km_entry_category(category_id,status,publish_time)` | 按分类过滤时命中 |
| 状态/时间筛选 | `idx_km_entry_publish(publish_time)` | 后台管理列表可用 |
| 版本号查询 | `uk_km_version_entry_no(entry_id,version_no)` | 单词条版本号唯一 |
| 浏览/收藏记录 | `idx_km_record_emp(emp_id,action_type)` | 用户历史推荐召回时命中 |
| 标签过滤 | `idx_km_tag_name(tag_name)` | 热门标签聚合时命中 |
| 推荐召回 | `idx_km_record_entry(entry_id,action_type)` | 找相似用户/相似词条 |

---

## 6. API 契约

统一前缀：`/api/knowledge`

| 方法 | 路径 | 权限码 | 说明 |
|------|------|--------|------|
| `POST` | `/api/knowledge/categories` | `km:category:create` | 创建分类 |
| `PUT` | `/api/knowledge/categories/{id}` | `km:category:update` | 更新分类 |
| `DELETE` | `/api/knowledge/categories/{id}` | `km:category:delete` | 删除分类 |
| `GET` | `/api/knowledge/categories` | `km:category:list` | 查询分类树 |
| `POST` | `/api/knowledge/entries` | `km:entry:create` | 创建词条（含第一版本） |
| `PUT` | `/api/knowledge/entries/{id}` | `km:entry:update` | 更新词条元信息 |
| `DELETE` | `/api/knowledge/entries/{id}` | `km:entry:delete` | 逻辑删除词条 |
| `GET` | `/api/knowledge/entries/{id}` | `km:entry:detail` | 查询词条详情 |
| `GET` | `/api/knowledge/entries` | `km:entry:list` | 分页查询词条 |
| `POST` | `/api/knowledge/entries/{id}/versions` | `km:entry:upload-version` | 上传新版本 |
| `GET` | `/api/knowledge/entries/{id}/versions` | `km:entry:version-list` | 查询版本历史 |
| `GET` | `/api/knowledge/entries/{id}/versions/{versionNo}/download` | `km:entry:download` | 下载指定版本 |
| `POST` | `/api/knowledge/entries/{id}/publish` | `km:entry:publish` | 发布词条 |
| `POST` | `/api/knowledge/entries/{id}/archive` | `km:entry:archive` | 归档词条 |
| `POST` | `/api/knowledge/entries/{id}/tags` | `km:tag:add` | 添加标签 |
| `DELETE` | `/api/knowledge/entries/{id}/tags/{tagId}` | `km:tag:remove` | 删除标签 |
| `GET` | `/api/knowledge/tags/hot` | `km:tag:hot` | 热门标签 TopN |
| `POST` | `/api/knowledge/entries/{id}/view` | `km:entry:view` | 记录浏览（自增计数） |
| `POST` | `/api/knowledge/entries/{id}/favorite` | `km:entry:favorite` | 收藏 |
| `DELETE` | `/api/knowledge/entries/{id}/favorite` | `km:entry:unfavorite` | 取消收藏 |
| `GET` | `/api/knowledge/entries/my/favorites` | `km:entry:my-favorite` | 我的收藏 |
| `POST` | `/api/knowledge/entries/{id}/rate` | `km:entry:rate` | 评分(1-5) |
| `POST` | `/api/knowledge/search` | `km:search:query` | 全文检索（ES） |
| `GET` | `/api/knowledge/recommend` | `km:recommend:list` | 推荐列表（协同过滤） |
| `GET` | `/api/knowledge/entries/{id}/related` | `km:entry:related` | 词条相关推荐 |

### 6.1 DTO/VO 字段

#### `KmEntryCreateDTO`

| 字段 | 类型 | 校验 |
|------|------|------|
| `title` | String | 必填，最长 200 |
| `summary` | String | 可选，最长 500 |
| `categoryId` | Long | 可选 |
| `deptId` | Long | 可选，不传则取当前用户部门 |
| `securityLevel` | String | 必填，枚举 `PUBLIC/INTERNAL/SECRET` |
| `tagNames` | List<String> | 可选，每个最长 32，去重 |
| `fileBase64` / `fileId` | String | 二选一必填（base64 或上传后文件ID） |
| `fileName` | String | 必填 |
| `versionComment` | String | 可选，最长 500 |

#### `KmEntryQueryDTO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `pageNum` / `pageSize` | Integer | 分页 |
| `keyword` | String | 模糊匹配标题/摘要 |
| `categoryId` | Long | 分类 |
| `deptId` | Long | 部门 |
| `status` | String | 状态 |
| `securityLevel` | String | 密级 |
| `tagName` | String | 标签 |
| `uploaderId` | Long | 创建人 |
| `sortField` | String | 排序字段，默认 `create_time` |
| `sortOrder` | String | `asc`/`desc`，默认 `desc` |

#### `KmSearchRequest`

| 字段 | 类型 | 说明 |
|------|------|------|
| `keyword` | String | 检索关键词（必填） |
| `categoryId` | Long | 可选过滤 |
| `deptId` | Long | 可选过滤 |
| `securityLevel` | String | 默认 `PUBLIC` |
| `tags` | List<String> | 标签 AND 过滤 |
| `pageNum` / `pageSize` | Integer | 分页 |
| `sortBy` | String | `relevance`/`view_count`/`update_time` |
| `highlight` | Boolean | 是否高亮，默认 true |

#### `KmSearchHitVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 词条ID |
| `title` | String | 标题 |
| `summary` | String | 摘要 |
| `tags` | List<String> | 标签 |
| `categoryId` / `categoryName` | Long/String | 分类 |
| `fileType` | String | 文件类型 |
| `viewCount` / `favoriteCount` / `ratingAvg` | Integer/Integer/Decimal | 统计 |
| `score` | Double | ES 相关性得分 |
| `highlights` | Map<String,List<String>> | 高亮片段 |
| `updateTime` | String | 更新时间 |

#### `KmRecommendVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `entryId` | Long | 词条ID |
| `title` | String | 标题 |
| `summary` | String | 摘要 |
| `reason` | String | 推荐原因（"看了A的用户也看B"、"标签命中：财务/制度"） |
| `score` | Double | 推荐综合得分 |
| `source` | String | `ITEM_CF` / `TAG_CF` / `HOT` |
| `tags` | List<String> | 标签 |

#### `KmEntryVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 词条ID |
| `title` | String | 标题 |
| `summary` | String | 摘要 |
| `currentVersion` | Integer | 当前版本号 |
| `status` / `statusName` | String/String | 状态 |
| `categoryId` / `categoryName` | Long/String | 分类 |
| `deptId` / `deptName` | Long/String | 部门 |
| `securityLevel` | String | 密级 |
| `viewCount` / `downloadCount` / `favoriteCount` | Integer | 计数 |
| `ratingAvg` / `ratingCount` | Decimal/Integer | 评分 |
| `tags` | List<KmTagVO> | 标签列表 |
| `uploaderId` / `uploaderName` | Long/String | 创建人 |
| `publisherId` / `publisherName` | Long/String | 发布人 |
| `publishTime` | String | 发布时间 |
| `isFavorited` | Boolean | 当前用户是否已收藏 |
| `myRating` | Integer | 当前用户评分（0=未评） |
| `canEdit` / `canDelete` / `canPublish` | Boolean | 当前用户权限 |

---

## 7. 推荐算法设计

### 7.1 三种召回策略

| 策略 | 说明 | 触发场景 |
|------|------|----------|
| HOT | 浏览/收藏/评分综合 TopN | 冷启动用户（新员工、历史行为 < 5 条） |
| TAG_CF | 标签命中相似词条，按命中标签数 × 标签权重排序 | 中等活跃用户 |
| ITEM_CF | 协同过滤：找相似用户看过的词条 | 活跃用户（历史行为 >= 10 条） |

### 7.2 协同过滤算法（简化版）

```
输入：
  - targetUserId
  - topN

步骤：
  1. 取 targetUser 最近 30 天浏览/收藏/评分记录（km_read_record）
  2. 按 entryId 聚合用户行为向量：weight = VIEW*1 + FAVORITE*3 + RATE*4
  3. 找 TopK 相似用户：
     a. 取所有在 targetUser 看过的 entryId 上有行为的其他用户
     b. 用余弦相似度 cos(user, target) 排序
     c. 取 Top 20 相似用户
  4. 候选词条 = 相似用户看过/收藏/评分、但 targetUser 未看过的 entryId
  5. 综合得分 = Σ (cos * 行为权重)
  6. 标签命中加权：+0.2 × (候选标签 ∩ target用户偏好标签数)
  7. 取 TopN

降级：
  - 若相似用户不足 5 人，回退到 TAG_CF
  - 仍不足，回退到 HOT
```

### 7.3 偏好标签提取

```
对 targetUser 最近 30 天所有浏览过的词条：
  统计每个 tag 出现次数 × 该 tag 所在词条停留时间系数
  取 Top 10 作为用户偏好标签
```

### 7.4 缓存策略

| Key | Value | TTL |
|-----|-------|-----|
| `km:recommend:user:{empId}` | List<KmRecommendVO> | 30 分钟 |
| `km:hot:entries` | List<KmEntryVO> | 10 分钟 |
| `km:tag:hot` | List<KmTagHotVO> | 10 分钟 |

---

## 8. 最小验收矩阵

| 区域 | 命令 |
|------|------|
| Knowledge 后端 | `cd code/backend && mvn -pl oa-knowledge -am test` |
| Knowledge + Web 入口 | `cd code/backend && mvn -pl oa-knowledge,oa-web -am test` |
| ES 同步集成 | `cd code/backend && mvn -pl oa-knowledge -am test -Dtest=KmEsSyncIT` |
| 推荐算法单测 | `cd code/backend && mvn -pl oa-knowledge -am test -Dtest=KmRecommendServiceTest` |
| 演示数据 | `mysql -u root -p oa_system < code/backend/sql/seed/004_seed_knowledge.sql` |
| 监控指标 | `curl http://localhost:8080/actuator/metrics/km.search.duration` |

---

## 9. Claude Code 提示词

### 9.1 T3 任务提示词（Entity + Mapper）

```text
请执行 oa-knowledge 模块重构 T3：知识库 Entity + Mapper。

必须先阅读：
- CLAUDE.md
- docs/superpowers/specs/2026-06-02-oa-system-redesign.md 第 3.2 节
- docs/superpowers/specs/2026-06-02-km-knowledge-task-split.md
- code/backend/sql/km_knowledge_contract.sql（待 T1 完成后存在）
- code/backend/oa-knowledge/pom.xml
- code/backend/oa-model/src/main/java/cn/oa/entity/ 下旧的 OaDocument / OaNotice 实体
- code/backend/oa-mapper 下相关 Mapper

范围：
- 只允许在 code/backend/oa-knowledge 模块内新增 Entity、Enum、Mapper、必要测试。
- 不实现 Service / Controller 业务逻辑。
- 不修改 frontend、mobile、正式 SQL baseline。
- 不删改 oa-model 旧实体。

输出物：
| 类型 | 建议名称 |
| Entity | KmEntry、KmVersion、KmTag、KmRelation、KmCategory、KmReadRecord |
| Enum | KmEntryStatus、KmSecurityLevel、KmRelationType、KmActionType、KmRelationAlgorithm |
| DTO | KmEntryCreateDTO、KmEntryUpdateDTO、KmEntryQueryDTO、KmVersionUploadDTO、KmSearchRequest、KmCategoryDTO、KmRateDTO |
| VO | KmEntryVO、KmVersionVO、KmTagVO、KmCategoryVO、KmSearchHitVO、KmRecommendVO、KmFavoriteVO |
| Mapper | KmEntryMapper、KmVersionMapper、KmTagMapper、KmRelationMapper、KmCategoryMapper、KmReadRecordMapper |

字段必须对齐 km_knowledge_contract.sql。
枚举覆盖 T1 文档中所有状态、密级、关联类型、动作类型。
DTO 必须包含基本 Jakarta Validation 注解。
Mapper 继承 MyBatis-Plus BaseMapper，可加必要自定义 SQL（如：浏览计数原子递增、聚合热门标签）。

完成后运行：
cd code/backend
mvn -pl oa-knowledge -am test

汇报：
- 新增/修改文件
- 是否发现已有重复知识库模型
- 验收命令结果
- T4 ES 集成需要的前置条件
```

### 9.2 T4 任务提示词（ES 索引 + 同步）

```text
请执行 oa-knowledge 模块重构 T4：Elasticsearch 索引 + 同步策略。

必须先阅读：
- T1 ES 索引 schema（km_document mapping）
- code/backend/oa-knowledge 模块结构（T3 完成后）
- oa-integration 中已存在的 ES 客户端封装（若无则本任务自建）
- Spring Data Elasticsearch / Elasticsearch Java Client 8.x 文档

范围：
- 在 oa-knowledge 中实现：
  * EsClientConfig（连接 ES 8.x）
  * KmIndexInitializer（启动时创建 km_document 索引，若已存在则检查 mapping 版本）
  * KmEsSyncService（提供 index/update/delete/bulkIndex 接口）
  * KmIndexEvent 领域事件 + ApplicationEventPublisher 监听器
  * WordExtractor 工具（Tika 提取 docx/pdf/xlsx/pptx/md 纯文本）
- 不修改 oa-integration 中通用 ES 配置。
- 不实现 Service 层业务逻辑，只实现同步基建。

事件：
- KmEntryCreatedEvent / KmEntryUpdatedEvent / KmEntryPublishedEvent /
  KmEntryArchivedEvent / KmEntryDeletedEvent / KmVersionUploadedEvent

每个事件由 KmEsSyncService 订阅并执行对应 ES 操作。

测试：
- 使用 Testcontainers 启动 ES 8.11 容器。
- 验证索引创建、CRUD 同步、删除同步、批量同步。

完成后运行：
cd code/backend
mvn -pl oa-knowledge -am test -Dtest='*Es*Test,*Index*Test'

汇报：
- 新增/修改文件
- 索引创建策略（首次/升级）
- 事件订阅清单
- Testcontainers 是否能成功启动
- T5 Service 调用同步的入口约定
```

### 9.3 T5 任务提示词（Service 层）

```text
请执行 oa-knowledge 模块重构 T5：知识库 Service 层。

必须先阅读：
- T3 Entity/DTO/VO/Enum/Mapper 结果
- T4 EsSyncService 接口
- code/backend/sql/km_knowledge_contract.sql
- 旧 oa-document Service 中的版本控制/审批流相关代码（仅作参考）
- docs/superpowers/specs/2026-06-02-oa-system-redesign.md 第 3.2 节

范围：
- 在 oa-knowledge 模块内实现：
  * KmEntryService（create/update/delete/getDetail/pageQuery/uploadVersion/publish/archive）
  * KmVersionService（listHistory/download/setCurrent）
  * KmTagService（addTag/removeTag/listByEntry/hotTags）
  * KmCategoryService（createCategory/updateCategory/deleteCategory/tree）
  * KmReadRecordService（recordView/recordDownload/recordFavorite/recordUnfavorite/recordRate/pageMyHistory）
  * KmFavoriteService + KmRatingService（独立 Service 也可）
- 发布 KmEntryCreatedEvent / KmEntryPublishedEvent / KmVersionUploadedEvent 等
- 浏览/下载/收藏计数使用原子 SQL：UPDATE km_entry SET view_count = view_count + 1 WHERE id = ?
- 不实现 REST Controller。
- 不直接依赖 ES Java Client 内部 API，必须通过 KmEsSyncService。

幂等要求：
- 同一用户对同一词条浏览 1 分钟内只记 1 次（用 Redis SETNX 标记）。
- 重复发布/重复归档要返回错误，不能覆盖。
- 评分更新使用 upsert 语义，不重复累加评分次数。

测试：
- 单元测试覆盖：词条创建/版本上传/发布/归档/标签增删/浏览计数/评分更新
- 使用 Mockito Mock KmEsSyncService、KmReadRecordService 等依赖

完成后运行：
cd code/backend
mvn -pl oa-knowledge -am test

汇报：
- 新增/修改文件
- 核心 Service 方法签名
- ES 同步事件发布清单
- 单元测试覆盖场景
- T6 推荐算法前置条件
```

### 9.4 T6 任务提示词（推荐算法）

```text
请执行 oa-knowledge 模块重构 T6：推荐算法 Service。

必须先阅读：
- T5 Service 结果（KmReadRecordService 数据来源）
- docs/superpowers/specs/2026-06-02-km-knowledge-task-split.md 第 7 节

范围：
- 在 oa-knowledge 模块下新建 recommend 包，实现：
  * RecommendStrategy 接口（recommend(Long empId, int topN): List<KmRecommendVO>）
  * HotRecommendStrategy（浏览/收藏/评分综合 TopN）
  * TagCFRecommendStrategy（标签命中相似词条）
  * ItemCFRecommendStrategy（协同过滤：找相似用户看过的词条）
  * RecommendService（按用户活跃度选择策略，结果合并去重 + 缓存到 Redis）
  * UserPreferenceService（提取用户偏好标签 Top10）
- 算法必须是可解释的：每条推荐都带 reason 字段说明来源。
- 缓存策略：推荐结果 30 分钟，热门列表 10 分钟。

测试：
- 冷启动：用户历史 < 5 条时使用 HOT
- 中等活跃：使用 TAG_CF
- 活跃用户：使用 ITEM_CF
- 推荐结果去重
- 降级：相似用户不足时回退到 TAG_CF / HOT

完成后运行：
cd code/backend
mvn -pl oa-knowledge -am test -Dtest='*Recommend*Test'

汇报：
- 新增/修改文件
- 三种策略的算法复杂度
- 缓存 Key 与 TTL
- 单元测试覆盖场景
- T8 Controller 接入方式
```

### 9.5 T7 任务提示词（基础 Controller）

```text
请执行 oa-knowledge 模块重构 T7：基础 REST API（词条/版本/标签/分类/浏览）。

必须先阅读：
- T5 Service 结果
- T1 文档第 6 节 API 契约（基础部分）
- code/backend/oa-web/src/main/java/cn/oa/controller 下现有 Controller（参考注解风格）

范围：
- 在 oa-knowledge 模块下实现：
  * KmEntryController（创建/更新/删除/详情/列表/上传版本/发布/归档）
  * KmVersionController（版本历史/下载）
  * KmTagController（添加/删除/热门）
  * KmCategoryController（CRUD/树形查询）
- OpenAPI/Knife4j 注解完整。
- 使用 @RequirePermission 注解。
- 普通用户只能操作自己创建的词条（canEdit/canDelete/canPublish 由 Service 校验）。
- 文件上传走 oa-web 已有 FileUploadController 风格（base64 或 fileId 模式任选）。
- Controller 不写业务逻辑，只做参数校验与权限检查。

测试：
- Controller 单元测试（使用 @WebMvcTest 或 MockMvc）
- 至少 12 个用例：词条 CRUD 各 1-2 个 + 版本/标签/分类各 2 个 + 权限检查

完成后运行：
cd code/backend
mvn -pl oa-knowledge,oa-web -am test

汇报：
- 新增/修改文件
- API 路径清单
- 权限码清单
- Controller 测试覆盖
- T8 前置条件
```

### 9.6 T8 任务提示词（检索/推荐/收藏/评分 Controller）

```text
请执行 oa-knowledge 模块重构 T8：检索/推荐/收藏/评分 REST API。

必须先阅读：
- T5 Service 结果
- T6 RecommendService 接口
- T7 Controller 风格

范围：
- 在 oa-knowledge 模块下实现：
  * KmSearchController（POST /api/knowledge/search，调用 ES 检索）
  * KmRecommendController（GET /api/knowledge/recommend，调用 RecommendService）
  * KmFavoriteController（POST/DELETE /api/knowledge/entries/{id}/favorite、GET /api/knowledge/entries/my/favorites）
  * KmRatingController（POST /api/knowledge/entries/{id}/rate）
  * KmRelatedController（GET /api/knowledge/entries/{id}/related）
- 检索结果需要带高亮（highlight）信息。
- 检索耗时通过 Micrometer Timer 记录。
- 评分（1-5）超出范围返回 400。
- 重复评分走 upsert，不重复计数。

测试：
- Controller 集成测试覆盖：检索分页/高亮/过滤、推荐三种策略入口、收藏幂等、评分范围
- 至少 10 个用例

完成后运行：
cd code/backend
mvn -pl oa-knowledge,oa-web -am test

汇报：
- 新增/修改文件
- API 路径清单
- 权限码清单
- Controller 测试覆盖
- T9 接口契约验证前置条件
```

### 9.7 T9 任务提示词（接口测试 + 权限）

```text
请执行 oa-knowledge 模块重构 T9：接口契约测试 + 权限码验证 + Knife4j 分组。

必须先阅读：
- T7/T8 Controller 结果
- T1 文档第 6 节 API 契约
- oa-web 现有 Knife4j 分组配置

范围：
- 在 oa-knowledge/src/test 下新增：
  * KmApiContractTest：使用 SpringBootTest + RestAssured/MockMvc 验证所有 API 路径、请求/响应字段、状态码
  * KmPermissionTest：使用 @WithMockUser 验证未授权用户访问受限接口返回 403
  * KmKnife4jTest：验证 /v3/api-docs/km 分组能正确导出
- 在 oa-web Knife4j 配置中新增 km 分组。
- 不修改 Service 业务规则。
- 不修改数据库。

测试：
- 每个 Controller 至少 2 个契约用例（成功 + 失败）
- 至少覆盖 8 个权限码

完成后运行：
cd code/backend
mvn -pl oa-knowledge -am test -Dtest='*ContractTest,*PermissionTest,*Knife4jTest'

汇报：
- 新增/修改文件
- 契约测试覆盖 API 数量
- 权限码覆盖数量
- Knife4j 分组结果
- T10 E2E 前置条件
```

### 9.8 T10 任务提示词（E2E + 演示数据）

```text
请执行 oa-knowledge 模块重构 T10：端到端测试 + 演示数据 seed。

必须先阅读：
- T1-T9 所有结果
- code/backend/sql/seed/ 下其他 seed 脚本（参考命名风格）

范围：
- 在 code/backend/sql/seed/ 下新增 004_seed_knowledge.sql，包含：
  * 5 个根分类 + 10 个子分类
  * 50 个常用标签
  * 20 个示例词条（覆盖制度、流程、模板、培训资料、FAQ）
  * 每个词条 1-2 个版本，混合 md/pdf/docx
  * 100 条浏览/收藏/评分记录（覆盖不同用户）
- 在 oa-knowledge/src/test/java/cn/oa/knowledge/e2e 下新增：
  * KmFullFlowE2E：登录 -> 创建词条 -> 上传版本 -> 发布 -> ES 同步 -> 检索 -> 推荐 -> 收藏 -> 评分
  * 使用 Testcontainers MySQL + ES + Redis
  * 至少 5 个场景用例
- 不修改正式 baseline，只新增 seed 脚本。
- 不删改 oa-knowledge Service / Controller。

完成后运行：
cd code/backend
mvn -pl oa-knowledge -P e2e test

汇报：
- 新增/修改文件
- 演示数据规模
- E2E 场景覆盖
- 验收命令结果
- T11 监控前置条件
```

### 9.9 T11 任务提示词（监控 + 验收清单）

```text
请执行 oa-knowledge 模块重构 T11：监控埋点 + 验收报告。

必须先阅读：
- T1-T10 所有结果
- oa-web 中 micrometer 配置

范围：
- 在 oa-knowledge 模块中：
  * KmMetrics 类：使用 Micrometer 注册 Counter（km.search.query.count、km.recommend.hit.count）和 Timer（km.search.duration、km.recommend.duration、km.es.sync.duration）
  * 在 KmSearchController / KmRecommendService / KmEsSyncService 中埋点
  * KmOperationLogAspect：使用 @OperationLog 注解记录关键操作（创建词条/上传版本/发布/删除）
- 在 docs/superpowers/specs/2026-06-02-km-knowledge-task-split.md 末尾追加：
  * 11 章 T11 执行结果
  * 12 章 整体验收报告
  * 13 章 旧入口下线清单（参考 T2 影响分析）
- 不修改 micrometer 全局配置。
- 不修改 oa-web Actuator 配置。

完成后运行：
cd code/backend
mvn -pl oa-knowledge -am test
curl -s http://localhost:8080/actuator/metrics/km.search.duration | jq .

汇报：
- 新增/修改文件
- 注册的指标名称与类型
- 旧入口下线清单
- 最终验收命令结果
- 模块整体完成度评估
```

---

## 10. 风险与回滚

| 风险 | 影响 | 缓解 |
|------|------|------|
| ES 同步失败导致数据不一致 | 检索结果与数据库不一致 | 同步失败重试 3 次 + 死信队列（`km:es:dlq` Redis List）+ 定时全量校对 |
| 推荐算法冷启动效果差 | 新用户无推荐 | 降级到 HOT 推荐 + 偏好标签引导用户 |
| 评分被刷 | 平均分失真 | 同一用户对同词条 24h 内多次评分只算 1 次；管理员可重置 |
| 词条删除牵连版本文件 | 磁盘泄漏 | 删除时软删 + 后台定时清理 90 天前 del_flag=1 的文件 |
| 标签爆炸 | 检索性能下降 | 标签合并建议：高频相似标签提示管理员合并 |
| 大文件上传 OOM | 服务异常 | 文件上传走分片/异步，不一次性读入内存 |
| 密级越权 | SECRET 词条被普通用户看到 | 检索时按 securityLevel 强过滤，ES 端应用 RBAC filter |

回滚点：

| 回滚场景 | 操作 |
|----------|------|
| ES 同步失败 | 关闭 `km.es.sync.enabled` 配置，词条仍可 CRUD，仅检索失败 |
| 推荐算法失败 | 关闭 `km.recommend.enabled`，前端展示空列表 |
| 词条创建失败 | 停止引用 `oa-knowledge` Controller，词条入口保留为只读 |
| ES 容器启动失败 | 自动 fallback 到 MySQL LIKE 检索（性能差但可用） |

---

## 11. 不在范围内但需要联调的事项

| 事项 | 负责方 | 联调时点 |
|------|--------|----------|
| Web 端知识库页面（列表/详情/编辑器） | 前端 | T7 完成后 |
| 移动端知识库页面（列表/详情/收藏） | 移动端 | T8 完成后 |
| 全文检索 UI（高亮、分页、过滤） | 前端 | T8 完成后 |
| 推荐位嵌入（首页/工作台） | 前端 | T6 完成后 |
| 上传组件（断点续传/秒传） | 前端 + 后端 fileId 模式 | T5 完成后 |
| 文档水印防泄密 | Web 共享组件 Watermark | 不在 oa-knowledge 范围 |
| AI 摘要/打标 | 后续 AI 中台 | 后续阶段 |

---

## 12. 验收 Checklist

- [ ] T1：6 张表 DDL + 索引 + ES mapping 文档化
- [ ] T2：旧实现影响清单
- [ ] T3：Entity/Mapper 单元测试通过
- [ ] T4：ES 索引创建、CRUD 同步、Testcontainers 测试通过
- [ ] T5：Service 单测覆盖（create/version/publish/archive/tag/count/rate）≥ 15 用例
- [ ] T6：推荐算法覆盖冷启动/HOT/TAG_CF/ITEM_CF 降级
- [ ] T7：基础 Controller 至少 12 个用例
- [ ] T8：检索/推荐/收藏/评分 Controller 至少 10 个用例
- [ ] T9：接口契约 + 权限 + Knife4j 分组测试通过
- [ ] T10：E2E 全流程 + 演示数据 seed 脚本可执行
- [ ] T11：监控指标注册成功 + 旧入口下线清单
- [ ] 整体：`mvn -pl oa-knowledge,oa-web -am test` 全部通过
- [ ] Knife4j `/doc.html` 中 km 分组可见且可调试
- [ ] Web/Mobile 端词条/检索/推荐页面可访问
- [ ] 监控指标 `/actuator/metrics/km.*` 可查询
