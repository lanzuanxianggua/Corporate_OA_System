package cn.oa.controller;

import cn.oa.common.result.R;
import cn.oa.entity.SysDept;
import cn.oa.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dept")
@Tag(name = "部门管理")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping("/tree")
    @Operation(summary = "获取部门树")
    public R<List<SysDept>> tree() {
        List<SysDept> tree = deptService.getDeptTree();
        return R.ok(tree);
    }

    @PostMapping
    @Operation(summary = "新增部门")
    public R<Void> add(@RequestBody SysDept dept) {
        deptService.save(dept);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改部门")
    public R<Void> update(@RequestBody SysDept dept) {
        deptService.updateById(dept);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除部门")
    public R<Void> delete(@PathVariable Long id) {
        deptService.removeById(id);
        return R.ok();
    }
}
