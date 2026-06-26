package cn.oa.service.impl;

import cn.oa.common.resolver.PermissionResolver;
import cn.oa.entity.SysEmpRole;
import cn.oa.entity.SysMenu;
import cn.oa.entity.SysRole;
import cn.oa.entity.SysRoleMenu;
import cn.oa.mapper.SysEmpRoleMapper;
import cn.oa.mapper.SysMenuMapper;
import cn.oa.mapper.SysRoleMapper;
import cn.oa.mapper.SysRoleMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Database permission resolver that queries sys_emp_role → sys_role → sys_role_menu → sys_menu.perms
 * and backfills the result into Redis cache.
 */
@Slf4j
@Component
public class PermissionResolverImpl implements PermissionResolver {

    @Autowired
    private SysEmpRoleMapper sysEmpRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<String> resolvePermissions(Long empId) {
        log.debug("PermissionResolver: resolving permissions from database for empId={}", empId);

        // 1. 查员工角色关联
        List<SysEmpRole> empRoles = sysEmpRoleMapper.selectList(
                new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getEmpId, empId));
        if (empRoles == null || empRoles.isEmpty()) {
            log.debug("PermissionResolver: no role mappings found for empId={}, returning empty", empId);
            return Collections.emptyList();
        }

        // 2. 查角色
        List<Long> roleIds = empRoles.stream()
                .map(SysEmpRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<SysRole> roles = roleIds.isEmpty() ? Collections.emptyList()
                : sysRoleMapper.selectBatchIds(roleIds);

        // 3. ADMIN 角色返回通配符
        boolean isAdmin = roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getRoleKey()));
        if (isAdmin) {
            List<String> adminPerms = List.of("*:*:*");
            cachePermissions(empId, adminPerms);
            return adminPerms;
        }

        // 4. 查角色-菜单关联
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus == null || roleMenus.isEmpty()) {
            return Collections.emptyList();
        }

        // 5. 查菜单上的权限标识
        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<SysMenu> menus = menuIds.isEmpty() ? Collections.emptyList()
                : sysMenuMapper.selectBatchIds(menuIds);

        // 6. 收集非空的权限标识
        List<String> permissions = menus.stream()
                .filter(menu -> menu.getPerms() != null && !menu.getPerms().isBlank())
                .map(SysMenu::getPerms)
                .distinct()
                .collect(Collectors.toList());

        // 7. 回填 Redis 缓存
        cachePermissions(empId, permissions);
        log.debug("PermissionResolver: resolved {} permissions for empId={}", permissions.size(), empId);
        return permissions;
    }

    private void cachePermissions(Long empId, List<String> permissions) {
        redisTemplate.opsForValue().set(
                "permissions:" + empId,
                permissions,
                2, TimeUnit.HOURS);
    }
}
