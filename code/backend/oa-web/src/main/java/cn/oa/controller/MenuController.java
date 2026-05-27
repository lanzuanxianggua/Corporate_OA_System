package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.R;
import cn.oa.entity.SysMenu;
import cn.oa.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@Tag(name = "菜单权限管理")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping("/tree")
    @Operation(summary = "获取菜单树")
    public R<List<SysMenu>> tree() {
        return R.ok(menuService.getMenuTree());
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "获取角色菜单ID列表")
    public R<List<Long>> roleMenus(@PathVariable Long roleId) {
        return R.ok(menuService.getMenuIdsByRoleId(roleId));
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增菜单")
    @OperationLog(module = "菜单管理", operation = "新增菜单")
    public R<Void> add(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return R.ok();
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改菜单")
    @OperationLog(module = "菜单管理", operation = "修改菜单")
    public R<Void> update(@RequestBody SysMenu menu) {
        menuService.updateById(menu);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除菜单")
    @OperationLog(module = "菜单管理", operation = "删除菜单")
    public R<Void> delete(@PathVariable Long id) {
        menuService.removeById(id);
        return R.ok();
    }

    @PutMapping("/role/{roleId}")
    @RequireAdmin
    @Operation(summary = "分配角色菜单")
    @OperationLog(module = "菜单管理", operation = "分配角色菜单")
    public R<Void> assignRoleMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        menuService.assignRoleMenus(roleId, menuIds);
        return R.ok();
    }
}
