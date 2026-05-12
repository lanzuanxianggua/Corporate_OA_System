package cn.oa.controller;

import cn.oa.common.result.R;
import cn.oa.entity.*;
import cn.oa.mapper.*;
import cn.oa.service.OperationLogService;
import cn.oa.service.impl.AuthServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
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

    @PostMapping("/user")
    public R<Map<String, Object>> userList(@RequestBody(required = false) Map<String, Object> params) {
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
    public R<List<Long>> listRoleIds(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        List<SysEmpRole> empRoles = empRoleMapper.selectList(
                new LambdaQueryWrapper<SysEmpRole>().eq(SysEmpRole::getEmpId, userId));
        List<Long> roleIds = empRoles.stream().map(SysEmpRole::getRoleId).collect(Collectors.toList());
        return R.ok(roleIds);
    }

    @PostMapping("/role")
    public R<Map<String, Object>> roleList(@RequestBody(required = false) Map<String, Object> params) {
        List<SysRole> roles = roleMapper.selectList(null);

        List<Map<String, Object>> list = roles.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("name", r.getRoleName());
            item.put("code", r.getRoleKey());
            item.put("status", 1);
            item.put("remark", r.getRoleName());
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

    @PostMapping("/menu")
    public R<List<Map<String, Object>>> menuList(@RequestBody(required = false) Map<String, Object> params) {
        return R.ok(buildMenuTree(0L));
    }

    @PostMapping("/dept")
    public R<List<Map<String, Object>>> deptList(@RequestBody(required = false) Map<String, Object> params) {
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
    public R<List<Map<String, Object>>> roleMenuList() {
        return R.ok(buildMenuTree(0L));
    }

    @PostMapping("/role-menu-ids")
    public R<List<Long>> roleMenuIds(@RequestBody Map<String, Object> params) {
        return R.ok(List.of(1L, 2L, 3L, 4L));
    }

    @GetMapping("/mine")
    public R<Map<String, Object>> mine(HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        String empName = (String) request.getAttribute("empName");

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
    public R<Map<String, Object>> mineLogs(@RequestParam(required = false) Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("id", i);
            log.put("ip", "192.168.1." + i);
            log.put("address", "内网");
            log.put("system", "Windows 11");
            log.put("browser", "Chrome 120");
            log.put("summary", "登录系统");
            log.put("operatingTime", "2024-01-0" + i + " 10:00:00");
            list.add(log);
        }
        result.put("list", list);
        result.put("total", list.size());
        result.put("pageSize", 10);
        result.put("currentPage", 1);
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
