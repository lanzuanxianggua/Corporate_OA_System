package cn.oa.common.resolver;

import java.util.List;

/**
 * Permission resolver for database fallback when Redis cache misses.
 * Implemented in oa-service module where mappers are available.
 *
 * <p>Resolves permissions (perms identifiers from sys_menu) for the given employee
 * by traversing sys_emp_role → sys_role_menu → sys_menu.perms, then backfills
 * the result into Redis cache.</p>
 */
public interface PermissionResolver {

    /**
     * Resolve permission identifiers for the given employee from database.
     *
     * @param empId employee ID
     * @return list of permission strings (e.g. ["admin:supply:list", "hr:employee:update"]),
     *         never null (empty list means no permissions, user can only access non-restricted endpoints)
     */
    List<String> resolvePermissions(Long empId);
}
