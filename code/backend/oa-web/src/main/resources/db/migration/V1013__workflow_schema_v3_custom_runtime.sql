-- V1013: custom workflow schema v3 support.
--
-- This migration keeps existing node_config runtime compatibility while adding
-- explicit storage slots for future designer/runtime/BPMN separation.
-- The current Java entity still uses node_config to avoid breaking databases
-- where ad-hoc migrations are applied manually.

ALTER TABLE wf_process_definition
  ADD COLUMN IF NOT EXISTS designer_schema LONGTEXT NULL COMMENT 'Frontend workflow designer schema JSON' AFTER node_config,
  ADD COLUMN IF NOT EXISTS runtime_schema LONGTEXT NULL COMMENT 'Backend normalized runtime schema JSON' AFTER designer_schema,
  ADD COLUMN IF NOT EXISTS bpmn_xml LONGTEXT NULL COMMENT 'Optional BPMN 2.0 XML export' AFTER runtime_schema;

UPDATE wf_process_definition
   SET node_config = JSON_SET(node_config, '$.schemaVersion', 3),
       update_by = 'system',
       update_time = NOW()
 WHERE del_flag = '0'
   AND JSON_VALID(node_config) = 1
   AND JSON_UNQUOTE(JSON_EXTRACT(node_config, '$.schemaVersion')) = '2';

UPDATE wf_process_definition
   SET designer_schema = JSON_SET(designer_schema, '$.schemaVersion', 3),
       update_by = 'system',
       update_time = NOW()
 WHERE del_flag = '0'
   AND JSON_VALID(designer_schema) = 1
   AND JSON_UNQUOTE(JSON_EXTRACT(designer_schema, '$.schemaVersion')) = '2';

UPDATE wf_process_definition
   SET runtime_schema = JSON_SET(runtime_schema, '$.schemaVersion', 3),
       update_by = 'system',
       update_time = NOW()
 WHERE del_flag = '0'
   AND JSON_VALID(runtime_schema) = 1
   AND JSON_UNQUOTE(JSON_EXTRACT(runtime_schema, '$.schemaVersion')) = '2';

UPDATE wf_process_definition
   SET process_key = REPLACE(process_key, '_v2', '_v3'),
       update_by = 'system',
       update_time = NOW()
 WHERE del_flag = '0'
   AND process_key LIKE '%\\_v2';

UPDATE wf_process_definition
   SET designer_schema = COALESCE(designer_schema, node_config),
       runtime_schema = COALESCE(runtime_schema, node_config)
 WHERE del_flag = '0';

-- Ensure only the newest active definition stays active for each process_type.
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
       older.update_time = NOW();
