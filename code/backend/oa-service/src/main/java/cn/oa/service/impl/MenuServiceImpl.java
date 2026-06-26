package cn.oa.service.impl;

import cn.oa.entity.SysEmpRole;
import cn.oa.entity.SysMenu;
import cn.oa.entity.SysRole;
import cn.oa.entity.SysRoleMenu;
import cn.oa.mapper.SysEmpRoleMapper;
import cn.oa.mapper.SysMenuMapper;
import cn.oa.mapper.SysRoleMapper;
import cn.oa.mapper.SysRoleMenuMapper;
import cn.oa.service.MenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements MenuService {

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysEmpRoleMapper sysEmpRoleMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<SysMenu> getMenuTree() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getOrderNum);
        return this.list(wrapper);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        if (isAdminRole(roleId)) {
            return getAllActiveMenuIds();
        }
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> list = roleMenuMapper.selectList(wrapper);
        return list.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignRoleMenus(Long roleId, List<Long> menuIds) {
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(wrapper);

        List<Long> effectiveMenuIds = isAdminRole(roleId) ? getAllActiveMenuIds() : menuIds;
        if (effectiveMenuIds != null && !effectiveMenuIds.isEmpty()) {
            for (Long menuId : effectiveMenuIds.stream().distinct().collect(Collectors.toList())) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }

        // 清除拥有该角色的所有用户的 permissions 缓存，下次请求时自动通过 PermissionResolver 重新加载
        List<SysEmpRole> empRoles = sysEmpRoleMapper.selectList(
                new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getRoleId, roleId));
        for (SysEmpRole er : empRoles) {
            redisTemplate.delete("permissions:" + er.getEmpId());
        }
    }

    private boolean isAdminRole(Long roleId) {
        if (roleId == null) return false;
        SysRole role = roleMapper.selectById(roleId);
        return role != null && "ADMIN".equalsIgnoreCase(role.getRoleKey());
    }

    private List<Long> getAllActiveMenuIds() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getStatus, "0")
                .orderByAsc(SysMenu::getOrderNum);
        return this.list(wrapper).stream()
                .map(SysMenu::getId)
                .collect(Collectors.toList());
    }
}
