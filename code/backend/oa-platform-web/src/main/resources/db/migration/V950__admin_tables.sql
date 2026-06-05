-- ============================================================
-- V950: 印章管理 + 资产管理 建表
-- ============================================================

-- 印章管理
CREATE TABLE adm_seal (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    seal_name       VARCHAR(100) NOT NULL              COMMENT '印章名称',
    seal_type       VARCHAR(20)  NOT NULL              COMMENT '类型: OFFICIAL/CONTRACT/FINANCE/PERSONAL',
    custodian       BIGINT       NOT NULL              COMMENT '保管人ID',
    dept_id         BIGINT       NOT NULL              COMMENT '所属部门ID',
    status          CHAR(10)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    location        VARCHAR(200) DEFAULT NULL           COMMENT '存放位置',
    del_flag        CHAR(1)      NOT NULL DEFAULT '0',
    create_by       VARCHAR(50),
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(50),
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_dept_id (dept_id),
    KEY idx_custodian (custodian),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='印章管理';

-- 资产管理
CREATE TABLE adm_asset (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    asset_code      VARCHAR(32)   NOT NULL              COMMENT '资产编号',
    asset_name      VARCHAR(100)  NOT NULL              COMMENT '资产名称',
    category        VARCHAR(20)   NOT NULL              COMMENT '分类: IT/OFFICE/VEHICLE/OTHER',
    brand           VARCHAR(50)   DEFAULT NULL           COMMENT '品牌',
    model           VARCHAR(50)   DEFAULT NULL           COMMENT '型号',
    purchase_date   DATE          DEFAULT NULL           COMMENT '购买日期',
    purchase_price  DECIMAL(12,2) DEFAULT NULL           COMMENT '购买价格',
    dept_id         BIGINT        NOT NULL               COMMENT '所属部门ID',
    custodian       BIGINT        NOT NULL               COMMENT '保管人ID',
    status          CHAR(10)      NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE/IN_USE/REPAIR/SCRAPPED',
    location        VARCHAR(200)  DEFAULT NULL           COMMENT '存放位置',
    del_flag        CHAR(1)       NOT NULL DEFAULT '0',
    create_by       VARCHAR(50),
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(50),
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_asset_code (asset_code),
    KEY idx_dept_status (dept_id, status),
    KEY idx_custodian (custodian)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产管理';

-- ============================================================
-- 权限数据 (admin 模块 10 个权限码)
-- ============================================================
INSERT INTO sys_permission (id, code, name, menu_path, sort_order, status) VALUES
(951, 'admin:seal:create',  '印章-新增', '/admin/seals',       1, '1'),
(952, 'admin:seal:update',  '印章-修改', '/admin/seals',       2, '1'),
(953, 'admin:seal:delete',  '印章-删除', '/admin/seals',       3, '1'),
(954, 'admin:seal:view',    '印章-查看', '/admin/seals',       4, '1'),
(955, 'admin:seal:list',    '印章-列表', '/admin/seals',       5, '1'),
(956, 'admin:asset:create', '资产-新增', '/admin/assets',      1, '1'),
(957, 'admin:asset:update', '资产-修改', '/admin/assets',      2, '1'),
(958, 'admin:asset:delete', '资产-删除', '/admin/assets',      3, '1'),
(959, 'admin:asset:view',   '资产-查看', '/admin/assets',      4, '1'),
(960, 'admin:asset:list',   '资产-列表', '/admin/assets',      5, '1');
