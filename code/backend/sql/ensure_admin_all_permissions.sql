-- Ensure ADMIN always has every active system menu permission.
-- Safe to rerun: only missing ADMIN/menu bindings are inserted.

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m
WHERE UPPER(r.role_key) = 'ADMIN'
  AND r.status = 0
  AND (r.del_flag IS NULL OR r.del_flag = '0')
  AND (m.status IS NULL OR m.status = '0')
  AND (m.del_flag IS NULL OR m.del_flag = '0')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = m.id
  );
