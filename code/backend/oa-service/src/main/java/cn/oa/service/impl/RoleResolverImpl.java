package cn.oa.service.impl;

import cn.oa.common.resolver.RoleResolver;
import cn.oa.entity.SysEmpRole;
import cn.oa.entity.SysRole;
import cn.oa.mapper.SysEmpRoleMapper;
import cn.oa.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Database role resolver that queries sys_emp_role + sys_role tables
 * and backfills the result into Redis cache.
 */
@Slf4j
@Component
public class RoleResolverImpl implements RoleResolver {

    @Autowired
    private SysEmpRoleMapper sysEmpRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<String> resolveRoles(Long empId) {
        log.debug("RoleResolver: resolving roles from database for empId={}", empId);

        List<SysEmpRole> empRoles = sysEmpRoleMapper.selectList(
                new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getEmpId, empId));

        List<String> roleKeys = new ArrayList<>();
        if (!empRoles.isEmpty()) {
            List<Long> roleIds = empRoles.stream()
                    .map(SysEmpRole::getRoleId)
                    .collect(Collectors.toList());
            List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
            roleKeys = roles.stream()
                    .map(SysRole::getRoleKey)
                    .collect(Collectors.toList());
            log.debug("RoleResolver: found {} roles for empId={}", roleKeys.size(), empId);
        } else {
            log.debug("RoleResolver: no role mappings found for empId={}", empId);
        }

        if (roleKeys.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("RoleResolver: defaulting to [USER] for empId={}", empId);
            }
            roleKeys.add("USER");
        }

        // Backfill Redis cache
        redisTemplate.opsForValue().set("roles:" + empId, roleKeys, 2, TimeUnit.HOURS);
        log.debug("RoleResolver: backfilled Redis cache roles:{} for empId={}", roleKeys, empId);

        return roleKeys;
    }
}
