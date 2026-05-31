package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.*;
import cn.oa.entity.dto.AssignRolesDTO;
import cn.oa.entity.dto.IdQueryDTO;
import cn.oa.mapper.*;
import cn.oa.service.OperationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Tag(name = "系统管理")
@SuppressWarnings({"unchecked", "deprecation"})
public class SystemManageController {

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private SysEmpRoleMapper empRoleMapper;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private OaLoginLogMapper loginLogMapper;

    @PostMapping("/user")
    @RequireAdmin
    @Operation(summary = "用户列表（分页）")
    public R<Map<String, Object>> userList(@RequestBody(required = false) @Valid Map<String, Object> params) {
        int pageNum = params != null && params.get("page") != null ? ((Number) params.get("page")).intValue() : 1;
        int pageSize = params != null && params.get("pageSize") != null ? ((Number) params.get("pageSize")).intValue() : 10;

        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        if (params != null && params.get("username") != null) {
            wrapper.like(SysEmployee::getEmpCode, params.get("username").toString());
        }
        if (params != null && params.get("status") != null) {
            wrapper.eq(SysEmployee::getStatus, params.get("status"));
        }
        wrapper.orderByDesc(SysEmployee::getCreateTime);

        Page<SysEmployee> page = employeeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (SysEmployee emp : page.getRecords()) {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", emp.getId());
            user.put("username", emp.getEmpCode());
            user.put("nickname", emp.getEmpName());
            user.put("phone", emp.getPhone());
            user.put("email", emp.getEmail());
            user.put("status", emp.getStatus());
            user.put("avatar", emp.getAvatar() != null ? emp.getAvatar() : "");
            user.put("createTime", emp.getCreateTime() != null ? emp.getCreateTime().toString() : "");

            if (emp.getDeptId() != null) {
                SysDept dept = deptMapper.selectById(emp.getDeptId());
                if (dept != null) {
                    Map<String, Object> deptMap = new LinkedHashMap<>();
                    deptMap.put("id", dept.getId());
                    deptMap.put("name", dept.getDeptName());
                    user.put("dept", deptMap);
                }
            }

            List<SysEmpRole> empRoles = empRoleMapper.selectList(
                    new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getEmpId, emp.getId()));
            if (!empRoles.isEmpty()) {
                List<Long> roleIds = empRoles.stream().map(SysEmpRole::getRoleId).collect(Collectors.toList());
                List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
                user.put("roles", roles.stream().map(r -> {
                    Map<String, Object> rm = new LinkedHashMap<>();
                    rm.put("id", r.getId());
                    rm.put("name", r.getRoleName());
                    rm.put("code", r.getRoleKey());
                    return rm;
                }).collect(Collectors.toList()));
            }

            list.add(user);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("pageSize", pageSize);
        result.put("currentPage", pageNum);
        return R.ok(result);
    }

    @GetMapping("/list-all-role")
    @RequireAdmin
    @Operation(summary = "获取所有角色")
    public R<List<Map<String, Object>>> listAllRole() {
        List<SysRole> roles = roleMapper.selectList(null);
        List<Map<String, Object>> list = roles.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("name", r.getRoleName());
            item.put("code", r.getRoleKey());
            return item;
        }).collect(Collectors.toList());
        return R.ok(list);
    }

    @PostMapping("/list-role-ids")
    @RequireAdmin
    @Operation(summary = "查询用户角色ID列表")
    public R<List<Long>> listRoleIds(@RequestBody @Valid IdQueryDTO dto) {
        Long userId = dto.getEffectiveId();
        List<SysEmpRole> empRoles = empRoleMapper.selectList(
                new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getEmpId, userId));
        List<Long> roleIds = empRoles.stream().map(SysEmpRole::getRoleId).collect(Collectors.toList());
        return R.ok(roleIds);
    }

    @PostMapping("/role")
    @RequireAdmin
    @Operation(summary = "角色列表")
    public R<Map<String, Object>> roleList(@RequestBody(required = false) @Valid Map<String, Object> params) {
        List<SysRole> roles = roleMapper.selectList(null);

        List<Map<String, Object>> list = roles.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("roleName", r.getRoleName());
            item.put("roleKey", r.getRoleKey());
            item.put("roleSort", r.getSort());
            item.put("status", r.getStatus());
            item.put("remark", r.getRemark());
            item.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : "");
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        result.put("pageSize", 10);
        result.put("currentPage", 1);
        return R.ok(result);
    }

    @PostMapping("/role/add")
    @RequireAdmin
    @OperationLog(module = "角色管理", operation = "新增角色")
    @Operation(summary = "新增角色")
    public R<Void> addRole(@RequestBody @Valid Map<String, Object> params) {
        String roleName = (String) params.get("roleName");
        String roleKey = (String) params.get("roleKey");
        if (roleName == null || roleName.isBlank() || roleKey == null || roleKey.isBlank()) {
            return R.fail("角色名称和角色标识不能为空");
        }
        SysRole role = new SysRole();
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        if (params.get("status") != null) {
            role.setStatus(((Number) params.get("status")).intValue());
        }
        if (params.get("sort") != null) {
            role.setSort(((Number) params.get("sort")).intValue());
        }
        if (params.get("remark") != null) {
            role.setRemark((String) params.get("remark"));
        }
        roleMapper.insert(role);
        log.info("Role created: roleName={}", roleName);
        return R.ok();
    }

    @PutMapping("/role/update")
    @RequireAdmin
    @OperationLog(module = "角色管理", operation = "修改角色")
    @Operation(summary = "修改角色")
    public R<Void> updateRole(@RequestBody @Valid Map<String, Object> params) {
        if (params.get("id") == null) {
            return R.fail("角色ID不能为空");
        }
        Long id = Long.valueOf(params.get("id").toString());
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            return R.fail("角色不存在");
        }
        if (params.get("roleName") != null) {
            role.setRoleName((String) params.get("roleName"));
        }
        if (params.get("roleKey") != null) {
            role.setRoleKey((String) params.get("roleKey"));
        }
        if (params.get("status") != null) {
            role.setStatus(((Number) params.get("status")).intValue());
        }
        if (params.get("sort") != null) {
            role.setSort(((Number) params.get("sort")).intValue());
        }
        if (params.get("remark") != null) {
            role.setRemark((String) params.get("remark"));
        }
        roleMapper.updateById(role);
        log.info("Role updated: id={}", id);
        return R.ok();
    }

    @DeleteMapping("/role/{id}")
    @RequireAdmin
    @OperationLog(module = "角色管理", operation = "删除角色")
    @Operation(summary = "删除角色")
    public R<Void> deleteRole(@PathVariable Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            return R.fail("角色不存在");
        }
        empRoleMapper.delete(new LambdaQueryWrapper<SysEmpRole>()
                .eq(SysEmpRole::getRoleId, id));
        roleMapper.deleteById(id);
        log.info("Role deleted: id={}", id);
        return R.ok();
    }

    @PostMapping("/menu")
    @RequireAdmin
    @Operation(summary = "菜单列表")
    public R<List<Map<String, Object>>> menuList(@RequestBody(required = false) @Valid Map<String, Object> params) {
        return R.ok(buildMenuTree(0L));
    }

    @PostMapping("/dept")
    @RequireAdmin
    @Operation(summary = "部门列表")
    public R<List<Map<String, Object>>> deptList(@RequestBody(required = false) @Valid Map<String, Object> params) {
        List<SysDept> allDepts = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));

        List<Map<String, Object>> list = new ArrayList<>();
        for (SysDept dept : allDepts) {
            if (dept.getParentId() == null || dept.getParentId() == 0) {
                list.add(buildDeptNode(dept, allDepts));
            }
        }
        return R.ok(list);
    }

    @PostMapping("/role-menu")
    @RequireAdmin
    @Operation(summary = "角色菜单列表")
    public R<List<Map<String, Object>>> roleMenuList() {
        return R.ok(buildMenuTree(0L));
    }

    @GetMapping("/api/system/roles")
    @Operation(summary = "获取所有角色(简单列表)")
    public R<List<SysRole>> getAllRoles() {
        return R.ok(roleMapper.selectList(null));
    }

    @PostMapping("/assign-roles")
    @RequireAdmin
    @OperationLog(module = "角色管理", operation = "分配角色")
    @Operation(summary = "分配用户角色")
    public R<Void> assignRoles(@RequestBody @Valid AssignRolesDTO dto) {
        empRoleMapper.delete(new LambdaQueryWrapper<SysEmpRole>()
            .eq(SysEmpRole::getEmpId, dto.getEmpId()));

        for (Long roleId : dto.getRoleIds()) {
            SysEmpRole er = new SysEmpRole();
            er.setEmpId(dto.getEmpId());
            er.setRoleId(roleId);
            empRoleMapper.insert(er);
        }
        log.info("Roles assigned: empId={}, roleIds={}", dto.getEmpId(), dto.getRoleIds());
        return R.ok();
    }

    @GetMapping("/emp-roles")
    @Operation(summary = "获取员工角色ID列表")
    public R<List<Long>> getEmpRoles(@RequestParam Long empId) {
        List<SysEmpRole> list = empRoleMapper.selectList(
            new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getEmpId, empId));
        return R.ok(list.stream().map(SysEmpRole::getRoleId).collect(Collectors.toList()));
    }

    @PostMapping("/role-menu-ids")
    @RequireAdmin
    @Operation(summary = "获取角色菜单ID列表")
    public R<List<Long>> roleMenuIds(@RequestBody @Valid IdQueryDTO dto) {
        Long roleId = dto.getEffectiveId();
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
        return R.ok(menuIds);
    }

    @GetMapping("/mine")
    @Operation(summary = "获取当前用户信息")
    public R<Map<String, Object>> mine(HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        String empName = WebUtil.getEmpName(request);

        SysEmployee emp = employeeMapper.selectById(empId);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("avatar", emp != null && emp.getAvatar() != null ? emp.getAvatar() : "");
        info.put("username", emp != null ? emp.getEmpCode() : "");
        info.put("nickname", empName);
        info.put("email", emp != null ? emp.getEmail() : "");
        info.put("phone", emp != null ? emp.getPhone() : "");
        info.put("description", "OA系统用户");
        return R.ok(info);
    }

    @GetMapping("/mine-logs")
    @Operation(summary = "获取当前用户登录日志")
    public R<Map<String, Object>> mineLogs(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);

        LambdaQueryWrapper<OaLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaLoginLog::getEmpId, empId)
                .orderByDesc(OaLoginLog::getLoginTime);

        Page<OaLoginLog> page = loginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream().map(log -> {
            Map<String, Object> logMap = new LinkedHashMap<>();
            logMap.put("id", log.getId());
            logMap.put("ip", log.getIp() != null ? log.getIp() : "");
            logMap.put("address", "");
            logMap.put("system", log.getOs() != null ? log.getOs() : "");
            logMap.put("browser", log.getBrowser() != null ? log.getBrowser() : "");
            logMap.put("summary", "登录系统");
            logMap.put("operatingTime", log.getLoginTime() != null ? log.getLoginTime().toString() : "");
            return logMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("pageSize", pageSize);
        result.put("currentPage", pageNum);
        return R.ok(result);
    }

    private Map<String, Object> buildDeptNode(SysDept dept, List<SysDept> allDepts) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", dept.getId());
        node.put("name", dept.getDeptName());
        node.put("parentId", dept.getParentId() != null ? dept.getParentId() : 0L);
        node.put("sort", dept.getSort());
        node.put("phone", dept.getPhone() != null ? dept.getPhone() : "");
        node.put("principal", dept.getLeader() != null ? dept.getLeader() : "");
        node.put("email", "");
        node.put("status", 1);
        node.put("type", dept.getParentId() != null && dept.getParentId() == 0 ? 1 : 3);
        node.put("createTime", dept.getCreateTime() != null ? dept.getCreateTime().toString() : "");
        node.put("remark", "");

        List<Map<String, Object>> children = new ArrayList<>();
        for (SysDept child : allDepts) {
            if (dept.getId().equals(child.getParentId())) {
                children.add(buildDeptNode(child, allDepts));
            }
        }
        if (!children.isEmpty()) {
            node.put("children", children);
        }
        return node;
    }

    private List<Map<String, Object>> buildMenuTree(Long parentId) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (parentId == 0L) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", 1L); m.put("parentId", 0L); m.put("menuType", 0);
            m.put("title", "menus.pureSysManagement"); m.put("name", "System");
            m.put("path", "/system"); m.put("component", "");
            m.put("rank", 10); m.put("icon", "ri:settings-3-line");
            m.put("showLink", true); m.put("keepAlive", false);
            m.put("children", buildMenuTree(1L));
            list.add(m);

            Map<String, Object> m2 = new LinkedHashMap<>();
            m2.put("id", 10L); m2.put("parentId", 0L); m2.put("menuType", 0);
            m2.put("title", "menus.pureSysMonitor"); m2.put("name", "Monitor");
            m2.put("path", "/monitor"); m2.put("component", "");
            m2.put("rank", 11); m2.put("icon", "ri:eye-line");
            m2.put("showLink", true); m2.put("keepAlive", false);
            m2.put("children", buildMenuTree(10L));
            list.add(m2);
        } else if (parentId == 1L) {
            String[][] items = {
                {"2", "menus.pureUser", "SystemUser", "/system/user/index", "system/user/index", "ri:admin-line"},
                {"3", "menus.pureRole", "SystemRole", "/system/role/index", "system/role/index", "ri:admin-line"},
                {"4", "menus.pureMenu", "SystemMenu", "/system/menu/index", "system/menu/index", "ri:admin-line"},
                {"5", "menus.pureDept", "SystemDept", "/system/dept/index", "system/dept/index", "ri:admin-line"}
            };
            for (String[] it : items) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", Long.parseLong(it[0])); m.put("parentId", 1L); m.put("menuType", 0);
                m.put("title", it[1]); m.put("name", it[2]);
                m.put("path", it[3]); m.put("component", it[4]);
                m.put("icon", it[5]); m.put("showLink", true); m.put("keepAlive", false);
                list.add(m);
            }
        } else if (parentId == 10L) {
            Map<String, Object> online = new LinkedHashMap<>();
            online.put("id", 11L); online.put("parentId", 10L); online.put("menuType", 0);
            online.put("title", "menus.pureOnlineUser"); online.put("name", "MonitorOnline");
            online.put("path", "/monitor/online/index"); online.put("component", "monitor/online/index");
            online.put("icon", "ri:eye-line"); online.put("showLink", true);
            list.add(online);
        }
        return list;
    }
}
