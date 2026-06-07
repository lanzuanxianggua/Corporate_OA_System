-- ============================================================
-- V989: 印章申请 + 资产领用 表 (oa-admin 工作流业务子表)
-- ============================================================

-- 1) 印章使用申请
CREATE TABLE adm_seal_applys (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    apply_no        VARCHAR(32)  NOT NULL              COMMENT '申请单号',
    seal_id         BIGINT       NOT NULL              COMMENT '印章 ID',
    emp_id          BIGINT       NOT NULL              COMMENT '申请人 emp_id',
    purpose         VARCHAR(500) NOT NULL              COMMENT '申请用途',
    doc_name        VARCHAR(200) NOT NULL              COMMENT '用印文件名称',
    doc_count       INT          NOT NULL DEFAULT 1    COMMENT '文件份数',
    expect_date     DATE         DEFAULT NULL          COMMENT '期望用印日期',
    use_date        DATE         DEFAULT NULL          COMMENT '实际用印日期',
    remark          VARCHAR(500) DEFAULT NULL          COMMENT '备注',
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING/APPROVED/REJECTED/USED/ARCHIVED',
    wf_instance_id  BIGINT       DEFAULT NULL          COMMENT '流程实例 ID',
    del_flag        CHAR(1)      NOT NULL DEFAULT '0',
    create_by       VARCHAR(50),
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(50),
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_apply_no (apply_no),
    KEY idx_seal (seal_id),
    KEY idx_emp (emp_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='印章使用申请表';

-- 2) 资产领用/归还
CREATE TABLE adm_asset_loans (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    loan_no              VARCHAR(32)  NOT NULL              COMMENT '领用单号',
    asset_id             BIGINT       NOT NULL              COMMENT '资产 ID',
    emp_id               BIGINT       NOT NULL              COMMENT '领用人 emp_id',
    loan_type            VARCHAR(16)  NOT NULL              COMMENT '类型: BORROW/RETURN/SCRAP',
    loan_date            DATE         DEFAULT NULL          COMMENT '领用日期',
    expect_return_date   DATE         DEFAULT NULL          COMMENT '预计归还日期',
    actual_return_date   DATE         DEFAULT NULL          COMMENT '实际归还日期',
    purpose              VARCHAR(500) NOT NULL              COMMENT '用途说明',
    remark               VARCHAR(500) DEFAULT NULL          COMMENT '备注',
    status               VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING/APPROVED/REJECTED/RETURNED',
    wf_instance_id       BIGINT       DEFAULT NULL          COMMENT '流程实例 ID',
    del_flag             CHAR(1)      NOT NULL DEFAULT '0',
    create_by            VARCHAR(50),
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by            VARCHAR(50),
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version              INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_loan_no (loan_no),
    KEY idx_asset (asset_id),
    KEY idx_emp (emp_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产领用/归还表';
