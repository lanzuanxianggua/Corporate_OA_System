-- ============================================
-- V961__fin_permissions.sql
-- 增量: 财务管理权限注册
-- ============================================

-- 1) 注册财务模块子菜单 + 按钮权限
INSERT INTO `sys_permission` (`parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort_order`, `status`, `create_by`) VALUES
  -- 子菜单
  (6,  'finance:budget',  '预算管理',  'MENU',   '/finance/budgets',   NULL, 20, 'ACTIVE', 'system'),
  (6,  'finance:expense', '费用报销',  'MENU',   '/finance/expenses',  NULL, 21, 'ACTIVE', 'system'),
  (6,  'finance:loan',    '借款管理',  'MENU',   '/finance/loans',     NULL, 22, 'ACTIVE', 'system'),
  -- 预算按钮
  (0,  'finance:budget:list',   '预算列表',   'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0,  'finance:budget:view',   '预算详情',   'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0,  'finance:budget:create', '预算新增',   'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0,  'finance:budget:update', '预算修改',   'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0,  'finance:budget:delete', '预算删除',   'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system'),
  (0,  'finance:budget:approve','预算审批',   'BUTTON', NULL, NULL, 6, 'ACTIVE', 'system'),
  -- 报销按钮
  (0,  'finance:expense:list',    '报销列表',   'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0,  'finance:expense:view',    '报销详情',   'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0,  'finance:expense:create',  '报销申请',   'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0,  'finance:expense:withdraw','报销撤回',   'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0,  'finance:expense:approve', '报销审批',   'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system'),
  -- 借款按钮
  (0,  'finance:loan:list',   '借款列表',   'BUTTON', NULL, NULL, 1, 'ACTIVE', 'system'),
  (0,  'finance:loan:view',   '借款详情',   'BUTTON', NULL, NULL, 2, 'ACTIVE', 'system'),
  (0,  'finance:loan:create', '借款申请',   'BUTTON', NULL, NULL, 3, 'ACTIVE', 'system'),
  (0,  'finance:loan:repay',  '借款还款',   'BUTTON', NULL, NULL, 4, 'ACTIVE', 'system'),
  (0,  'finance:loan:approve','借款审批',   'BUTTON', NULL, NULL, 5, 'ACTIVE', 'system');

-- 2) 给 SUPER_ADMIN (role_id=1) 分配所有财务权限
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`, `create_by`)
  SELECT 1, id, 'system'
  FROM `sys_permission`
  WHERE `perm_code` IN (
    'finance:budget', 'finance:expense', 'finance:loan',
    'finance:budget:list', 'finance:budget:view', 'finance:budget:create', 'finance:budget:update', 'finance:budget:delete', 'finance:budget:approve',
    'finance:expense:list', 'finance:expense:view', 'finance:expense:create', 'finance:expense:withdraw', 'finance:expense:approve',
    'finance:loan:list', 'finance:loan:view', 'finance:loan:create', 'finance:loan:repay', 'finance:loan:approve'
  ) AND `del_flag` = '0';
