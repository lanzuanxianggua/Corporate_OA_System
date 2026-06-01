package cn.oa.common.resolver;

import java.util.List;

/**
 * Role resolver for database fallback when Redis cache misses.
 * Implemented in oa-service module where mappers are available.
 */
public interface RoleResolver {

    /**
     * Resolve roles for the given employee from database,
     * then backfill into Redis cache.
     *
     * @param empId employee ID
     * @return list of role keys, never null (defaults to ["USER"] if no roles found)
     */
    List<String> resolveRoles(Long empId);
}
