package cn.oa.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bootstraps the current workflow schema against the configured MySQL datasource.
 *
 * <p>The workflow runtime depends on persistent MySQL tables wf_task and
 * wf_delegation. This runner creates and normalizes those real database tables
 * only; it never uses temporary tables, in-memory state, or mock data.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class WorkflowSchemaBootstrapInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${oa.schema.bootstrap-workflow-tables:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Persistent workflow schema bootstrap disabled");
            return;
        }

        createWorkflowTablesIfMissing();
        enforceWorkflowDefinitionSchema();
        enforceWorkflowTaskSchema();
        enforceWorkflowDelegationSchema();
        dropRetiredWorkflowTables();
        log.info("Persistent workflow schema bootstrap completed");
    }

    private void createWorkflowTablesIfMissing() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wf_process_definition (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  process_name VARCHAR(100) NOT NULL,
                  process_key VARCHAR(100) NOT NULL,
                  process_type VARCHAR(50) NOT NULL,
                  node_config LONGTEXT NOT NULL,
                  designer_schema LONGTEXT DEFAULT NULL,
                  runtime_schema LONGTEXT DEFAULT NULL,
                  bpmn_xml LONGTEXT DEFAULT NULL,
                  status VARCHAR(20) NOT NULL DEFAULT '0',
                  version INT NOT NULL DEFAULT 1,
                  create_by VARCHAR(64) DEFAULT NULL,
                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  update_by VARCHAR(64) DEFAULT NULL,
                  update_time DATETIME DEFAULT NULL,
                  del_flag CHAR(1) NOT NULL DEFAULT '0',
                  PRIMARY KEY (id),
                  KEY idx_process_type_status (process_type, status, del_flag),
                  KEY idx_process_key_version (process_key, version),
                  KEY idx_process_type_version (process_type, version)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wf_task (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  instance_id BIGINT NOT NULL,
                  node_id BIGINT DEFAULT NULL,
                  node_name VARCHAR(100) DEFAULT NULL,
                  assignee_id BIGINT NOT NULL,
                  task_type VARCHAR(20) NOT NULL DEFAULT 'TODO',
                  parent_task_id BIGINT DEFAULT NULL,
                  status VARCHAR(20) NOT NULL DEFAULT '0',
                  opinion VARCHAR(500) DEFAULT NULL,
                  signature VARCHAR(200) DEFAULT NULL,
                  due_time DATETIME DEFAULT NULL,
                  complete_time DATETIME DEFAULT NULL,
                  remind_count INT NOT NULL DEFAULT 0,
                  escalation_count INT NOT NULL DEFAULT 0,
                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  KEY idx_instance_id (instance_id),
                  KEY idx_assignee_id (assignee_id),
                  KEY idx_node_id (node_id),
                  KEY idx_due_time (due_time),
                  KEY idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wf_delegation (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  delegator_id BIGINT NOT NULL,
                  delegate_id BIGINT NOT NULL,
                  process_category VARCHAR(32) DEFAULT NULL,
                  notify_delegator TINYINT(1) NOT NULL DEFAULT 1,
                  start_date DATE NOT NULL,
                  end_date DATE NOT NULL,
                  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                  create_by VARCHAR(64) DEFAULT NULL,
                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  KEY idx_delegator_id (delegator_id),
                  KEY idx_delegate_id (delegate_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void enforceWorkflowDefinitionSchema() {
        addColumnIfMissing("wf_process_definition", "process_key",
                "ALTER TABLE wf_process_definition ADD COLUMN process_key VARCHAR(100) NOT NULL DEFAULT '' AFTER process_name");
        addColumnIfMissing("wf_process_definition", "process_type",
                "ALTER TABLE wf_process_definition ADD COLUMN process_type VARCHAR(50) NOT NULL DEFAULT '' AFTER process_key");
        addColumnIfMissing("wf_process_definition", "node_config",
                "ALTER TABLE wf_process_definition ADD COLUMN node_config LONGTEXT NULL AFTER process_type");
        addColumnIfMissing("wf_process_definition", "designer_schema",
                "ALTER TABLE wf_process_definition ADD COLUMN designer_schema LONGTEXT NULL COMMENT 'Frontend workflow designer schema JSON' AFTER node_config");
        addColumnIfMissing("wf_process_definition", "runtime_schema",
                "ALTER TABLE wf_process_definition ADD COLUMN runtime_schema LONGTEXT NULL COMMENT 'Backend normalized runtime schema JSON' AFTER designer_schema");
        addColumnIfMissing("wf_process_definition", "bpmn_xml",
                "ALTER TABLE wf_process_definition ADD COLUMN bpmn_xml LONGTEXT NULL COMMENT 'Optional BPMN 2.0 XML export' AFTER runtime_schema");

        upgradeWorkflowJsonColumnToV3("node_config");
        upgradeWorkflowJsonColumnToV3("designer_schema");
        upgradeWorkflowJsonColumnToV3("runtime_schema");
        jdbcTemplate.update("""
                UPDATE wf_process_definition
                   SET process_key = REPLACE(process_key, '_v2', '_v3'),
                       update_by = 'system',
                       update_time = NOW()
                 WHERE del_flag = '0'
                   AND process_key LIKE '%\\_v2'
                """);

        jdbcTemplate.update("""
                UPDATE wf_process_definition
                   SET designer_schema = COALESCE(designer_schema, node_config),
                       runtime_schema = COALESCE(runtime_schema, node_config)
                 WHERE del_flag = '0'
                """);

        jdbcTemplate.update("""
                UPDATE wf_process_definition older
                JOIN wf_process_definition newer
                  ON older.process_type = newer.process_type
                 AND older.del_flag = '0'
                 AND newer.del_flag = '0'
                 AND older.status = '0'
                 AND newer.status = '0'
                 AND older.version < newer.version
                   SET older.status = '1',
                       older.update_by = 'system',
                       older.update_time = NOW()
                """);

        addIndexIfMissing("wf_process_definition", "idx_process_type_status",
                "ALTER TABLE wf_process_definition ADD INDEX idx_process_type_status (process_type, status, del_flag)");
        addIndexIfMissing("wf_process_definition", "idx_process_key_version",
                "ALTER TABLE wf_process_definition ADD INDEX idx_process_key_version (process_key, version)");
        addIndexIfMissing("wf_process_definition", "idx_process_type_version",
                "ALTER TABLE wf_process_definition ADD INDEX idx_process_type_version (process_type, version)");
    }

    private void upgradeWorkflowJsonColumnToV3(String column) {
        if (!columnExists("wf_process_definition", column)) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE wf_process_definition
                   SET %s = JSON_SET(%s, '$.schemaVersion', 3),
                       update_by = 'system',
                       update_time = NOW()
                 WHERE del_flag = '0'
                   AND JSON_VALID(%s) = 1
                   AND JSON_UNQUOTE(JSON_EXTRACT(%s, '$.schemaVersion')) = '2'
                """.formatted(column, column, column, column));
    }

    private void enforceWorkflowTaskSchema() {
        addColumnIfMissing("wf_task", "node_id", "ALTER TABLE wf_task ADD COLUMN node_id BIGINT DEFAULT NULL AFTER instance_id");
        addColumnIfMissing("wf_task", "node_name", "ALTER TABLE wf_task ADD COLUMN node_name VARCHAR(100) DEFAULT NULL AFTER node_id");
        addColumnIfMissing("wf_task", "task_type", "ALTER TABLE wf_task ADD COLUMN task_type VARCHAR(20) NOT NULL DEFAULT 'TODO' AFTER assignee_id");
        addColumnIfMissing("wf_task", "parent_task_id", "ALTER TABLE wf_task ADD COLUMN parent_task_id BIGINT DEFAULT NULL AFTER task_type");
        addColumnIfMissing("wf_task", "opinion", "ALTER TABLE wf_task ADD COLUMN opinion VARCHAR(500) DEFAULT NULL AFTER status");
        addColumnIfMissing("wf_task", "signature", "ALTER TABLE wf_task ADD COLUMN signature VARCHAR(200) DEFAULT NULL AFTER opinion");
        addColumnIfMissing("wf_task", "due_time", "ALTER TABLE wf_task ADD COLUMN due_time DATETIME DEFAULT NULL AFTER signature");
        addColumnIfMissing("wf_task", "complete_time", "ALTER TABLE wf_task ADD COLUMN complete_time DATETIME DEFAULT NULL AFTER due_time");
        addColumnIfMissing("wf_task", "remind_count", "ALTER TABLE wf_task ADD COLUMN remind_count INT NOT NULL DEFAULT 0 AFTER complete_time");
        addColumnIfMissing("wf_task", "escalation_count", "ALTER TABLE wf_task ADD COLUMN escalation_count INT NOT NULL DEFAULT 0 AFTER remind_count");
        addColumnIfMissing("wf_task", "create_time", "ALTER TABLE wf_task ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

        migrateColumnIfPresent("wf_task", "node_index", "node_id", "UPDATE wf_task SET node_id = node_index WHERE node_id IS NULL AND node_index IS NOT NULL");
        migrateColumnIfPresent("wf_task", "remark", "opinion", "UPDATE wf_task SET opinion = remark WHERE opinion IS NULL AND remark IS NOT NULL");
        migrateColumnIfPresent("wf_task", "action_time", "complete_time", "UPDATE wf_task SET complete_time = action_time WHERE complete_time IS NULL AND action_time IS NOT NULL");
        migrateColumnIfPresent("wf_task", "deadline", "due_time", "UPDATE wf_task SET due_time = deadline WHERE due_time IS NULL AND deadline IS NOT NULL");

        jdbcTemplate.update("UPDATE wf_task SET task_type = 'TODO' WHERE task_type IS NULL OR task_type = ''");
        jdbcTemplate.update("UPDATE wf_task SET assignee_id = 0 WHERE assignee_id IS NULL");

        modifyColumnIfExists("wf_task", "instance_id", "ALTER TABLE wf_task MODIFY COLUMN instance_id BIGINT NOT NULL");
        modifyColumnIfExists("wf_task", "assignee_id", "ALTER TABLE wf_task MODIFY COLUMN assignee_id BIGINT NOT NULL");
        modifyColumnIfExists("wf_task", "task_type", "ALTER TABLE wf_task MODIFY COLUMN task_type VARCHAR(20) NOT NULL DEFAULT 'TODO'");
        modifyColumnIfExists("wf_task", "status", "ALTER TABLE wf_task MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT '0'");
        modifyColumnIfExists("wf_task", "remind_count", "ALTER TABLE wf_task MODIFY COLUMN remind_count INT NOT NULL DEFAULT 0");
        modifyColumnIfExists("wf_task", "escalation_count", "ALTER TABLE wf_task MODIFY COLUMN escalation_count INT NOT NULL DEFAULT 0");
        modifyColumnIfExists("wf_task", "create_time", "ALTER TABLE wf_task MODIFY COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

        dropColumnIfExists("wf_task", "process_id");
        dropColumnIfExists("wf_task", "node_index");
        dropColumnIfExists("wf_task", "action_time");
        dropColumnIfExists("wf_task", "remark");
        dropColumnIfExists("wf_task", "last_remind_time");
        dropColumnIfExists("wf_task", "deadline");
        dropColumnIfExists("wf_task", "action_source");
        dropColumnIfExists("wf_task", "multi_type");

        addIndexIfMissing("wf_task", "idx_instance_id", "ALTER TABLE wf_task ADD INDEX idx_instance_id (instance_id)");
        addIndexIfMissing("wf_task", "idx_assignee_id", "ALTER TABLE wf_task ADD INDEX idx_assignee_id (assignee_id)");
        addIndexIfMissing("wf_task", "idx_node_id", "ALTER TABLE wf_task ADD INDEX idx_node_id (node_id)");
        addIndexIfMissing("wf_task", "idx_due_time", "ALTER TABLE wf_task ADD INDEX idx_due_time (due_time)");
        addIndexIfMissing("wf_task", "idx_status", "ALTER TABLE wf_task ADD INDEX idx_status (status)");
        addIndexIfMissing("wf_task", "idx_assignee_status_create", "ALTER TABLE wf_task ADD INDEX idx_assignee_status_create (assignee_id, status, create_time)");
        addIndexIfMissing("wf_task", "idx_assignee_status_complete", "ALTER TABLE wf_task ADD INDEX idx_assignee_status_complete (assignee_id, status, complete_time)");

        if (tableExists("oa_approval_record")) {
            addIndexIfMissing("oa_approval_record", "idx_approver_task", "ALTER TABLE oa_approval_record ADD INDEX idx_approver_task (approver_id, task_id)");
        }
    }

    private void enforceWorkflowDelegationSchema() {
        addColumnIfMissing("wf_delegation", "process_category", "ALTER TABLE wf_delegation ADD COLUMN process_category VARCHAR(32) DEFAULT NULL AFTER delegate_id");
        addColumnIfMissing("wf_delegation", "notify_delegator", "ALTER TABLE wf_delegation ADD COLUMN notify_delegator TINYINT(1) NOT NULL DEFAULT 1 AFTER process_category");
        addColumnIfMissing("wf_delegation", "start_date", "ALTER TABLE wf_delegation ADD COLUMN start_date DATE DEFAULT NULL");
        addColumnIfMissing("wf_delegation", "end_date", "ALTER TABLE wf_delegation ADD COLUMN end_date DATE DEFAULT NULL");
        addColumnIfMissing("wf_delegation", "create_by", "ALTER TABLE wf_delegation ADD COLUMN create_by VARCHAR(64) DEFAULT NULL AFTER status");
        addColumnIfMissing("wf_delegation", "create_time", "ALTER TABLE wf_delegation ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

        migrateColumnIfPresent("wf_delegation", "business_type", "process_category",
                "UPDATE wf_delegation SET process_category = business_type WHERE process_category IS NULL AND business_type IS NOT NULL");
        migrateColumnIfPresent("wf_delegation", "start_time", "start_date",
                "UPDATE wf_delegation SET start_date = DATE(start_time) WHERE start_date IS NULL AND start_time IS NOT NULL");
        migrateColumnIfPresent("wf_delegation", "end_time", "end_date",
                "UPDATE wf_delegation SET end_date = DATE(end_time) WHERE end_date IS NULL AND end_time IS NOT NULL");

        jdbcTemplate.update("UPDATE wf_delegation SET status = 'ACTIVE' WHERE status = '0'");
        jdbcTemplate.update("UPDATE wf_delegation SET status = 'CANCELLED' WHERE status IN ('1', '2')");
        jdbcTemplate.update("UPDATE wf_delegation SET start_date = CURRENT_DATE WHERE start_date IS NULL");
        jdbcTemplate.update("UPDATE wf_delegation SET end_date = start_date WHERE end_date IS NULL");

        modifyColumnIfExists("wf_delegation", "delegator_id", "ALTER TABLE wf_delegation MODIFY COLUMN delegator_id BIGINT NOT NULL");
        modifyColumnIfExists("wf_delegation", "delegate_id", "ALTER TABLE wf_delegation MODIFY COLUMN delegate_id BIGINT NOT NULL");
        modifyColumnIfExists("wf_delegation", "notify_delegator", "ALTER TABLE wf_delegation MODIFY COLUMN notify_delegator TINYINT(1) NOT NULL DEFAULT 1");
        modifyColumnIfExists("wf_delegation", "start_date", "ALTER TABLE wf_delegation MODIFY COLUMN start_date DATE NOT NULL");
        modifyColumnIfExists("wf_delegation", "end_date", "ALTER TABLE wf_delegation MODIFY COLUMN end_date DATE NOT NULL");
        modifyColumnIfExists("wf_delegation", "status", "ALTER TABLE wf_delegation MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
        modifyColumnIfExists("wf_delegation", "create_time", "ALTER TABLE wf_delegation MODIFY COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");

        dropColumnIfExists("wf_delegation", "business_type");
        dropColumnIfExists("wf_delegation", "start_time");
        dropColumnIfExists("wf_delegation", "end_time");
        dropColumnIfExists("wf_delegation", "update_time");

        addIndexIfMissing("wf_delegation", "idx_delegator_id", "ALTER TABLE wf_delegation ADD INDEX idx_delegator_id (delegator_id)");
        addIndexIfMissing("wf_delegation", "idx_delegate_id", "ALTER TABLE wf_delegation ADD INDEX idx_delegate_id (delegate_id)");
    }

    private void dropRetiredWorkflowTables() {
        dropTableIfExists("wf_assignee_rule");
        dropTableIfExists("wf_transition");
        dropTableIfExists("wf_record");
        dropTableIfExists("wf_node");
        dropTableIfExists("wf_instance");
        dropTableIfExists("wf_definition");
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        if (!columnExists(table, column)) {
            jdbcTemplate.execute(ddl);
            log.info("Workflow schema added column {}.{}", table, column);
        }
    }

    private void modifyColumnIfExists(String table, String column, String ddl) {
        if (columnExists(table, column)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void migrateColumnIfPresent(String table, String sourceColumn, String targetColumn, String sql) {
        if (columnExists(table, sourceColumn) && columnExists(table, targetColumn)) {
            jdbcTemplate.update(sql);
        }
    }

    private void dropColumnIfExists(String table, String column) {
        if (columnExists(table, column)) {
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP COLUMN " + column);
            log.info("Workflow schema dropped old column {}.{}", table, column);
        }
    }

    private void addIndexIfMissing(String table, String indexName, String ddl) {
        if (!indexExists(table, indexName)) {
            jdbcTemplate.execute(ddl);
            log.info("Workflow schema added index {}.{}", table, indexName);
        }
    }

    private void dropTableIfExists(String table) {
        if (tableExists(table)) {
            jdbcTemplate.execute("DROP TABLE " + table);
            log.info("Workflow schema dropped retired table {}", table);
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    private boolean indexExists(String table, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """, Integer.class, table, indexName);
        return count != null && count > 0;
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """, Integer.class, table);
        return count != null && count > 0;
    }
}
