package cn.oa.system.controller;

import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 员工/用户 Controller.
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/emps")
public class SysEmpController {

    private final AuthService authService;
    private final cn.oa.system.mapper.SysEmpMapper empMapper;

    public SysEmpController(AuthService authService, cn.oa.system.mapper.SysEmpMapper empMapper) {
        this.authService = authService;
        this.empMapper = empMapper;
    }

    @Operation(summary = "员工分页")
    @GetMapping
    @RequirePermission("system:user:list")
    public R<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {

        var wrapper = new LambdaQueryWrapper<SysEmp>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysEmp::getUsername, keyword)
                    .or().like(SysEmp::getRealName, keyword)
                    .or().like(SysEmp::getEmpCode, keyword));
        }
        wrapper.orderByDesc(SysEmp::getCreateTime);

        Page<SysEmp> page = empMapper.selectPage(Page.of(pageNum, pageSize), wrapper);

        List<Map<String, Object>> records = page.getRecords().stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("list", records);
        data.put("total", page.getTotal());
        data.put("pageNum", page.getCurrent());
        data.put("pageSize", page.getSize());
        return R.ok(data);
    }

    @Operation(summary = "员工详情")
    @GetMapping("/{id}")
    @RequirePermission("system:user:view")
    public R<Map<String, Object>> get(@PathVariable Long id) {
        SysEmp emp = empMapper.selectById(id);
        if (emp == null) {
            return R.fail(101, "员工不存在");
        }
        return R.ok(toMap(emp));
    }

    private Map<String, Object> toMap(SysEmp emp) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", emp.getId());
        map.put("empCode", emp.getEmpCode());
        map.put("username", emp.getUsername());
        map.put("realName", emp.getRealName());
        map.put("email", emp.getEmail());
        map.put("phone", emp.getPhone());
        map.put("avatar", emp.getAvatar());
        map.put("deptId", emp.getDeptId());
        map.put("position", emp.getPosition());
        map.put("status", emp.getStatus());
        map.put("createTime", emp.getCreateTime());
        return map;
    }
}
