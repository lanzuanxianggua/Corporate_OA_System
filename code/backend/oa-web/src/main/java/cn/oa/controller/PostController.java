package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.SysPost;
import cn.oa.service.PostService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/post")
@Tag(name = "岗位管理")
public class PostController {

    @Autowired
    private PostService postService;

    @GetMapping("/page")
    @Operation(summary = "岗位分页列表")
    public R<PageResult<SysPost>> page(@RequestParam int pageNum,
                                       @RequestParam int pageSize,
                                       @RequestParam(required = false) String postName,
                                       @RequestParam(required = false) String postCode) {
        IPage<SysPost> page = postService.pageList(pageNum, pageSize, postName, postCode);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/list")
    @Operation(summary = "所有岗位列表（下拉选择用）")
    public R<List<SysPost>> list() {
        return R.ok(postService.list());
    }

    @PostMapping
    @Operation(summary = "新增岗位")
    @RequireAdmin
    public R<Void> add(@RequestBody SysPost post) {
        postService.save(post);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改岗位")
    @RequireAdmin
    public R<Void> update(@RequestBody SysPost post) {
        postService.updateById(post);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除岗位")
    @RequireAdmin
    public R<Void> delete(@PathVariable Long id) {
        postService.removeById(id);
        return R.ok();
    }
}
