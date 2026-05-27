package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaAsset;
import cn.oa.entity.OaAssetBorrow;
import cn.oa.service.AssetBorrowService;
import cn.oa.service.AssetService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asset")
@Tag(name = "资产管理")
public class AssetController {

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetBorrowService assetBorrowService;

    @GetMapping("/page")
    @Operation(summary = "分页查询资产")
    public R<PageResult<OaAsset>> page(@RequestParam int pageNum,
                                        @RequestParam int pageSize,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) Character status) {
        IPage<OaAsset> page = assetService.pageList(pageNum, pageSize, category, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增资产")
    public R<Void> add(@RequestBody OaAsset asset) {
        assetService.save(asset);
        return R.ok();
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改资产")
    public R<Void> update(@RequestBody OaAsset asset) {
        assetService.updateById(asset);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除资产")
    public R<Void> delete(@PathVariable Long id) {
        assetService.removeById(id);
        return R.ok();
    }

    @PostMapping("/borrow")
    @Operation(summary = "借出资产")
    public R<Void> borrow(@RequestBody OaAssetBorrow borrow) {
        assetBorrowService.borrowAsset(borrow);
        return R.ok();
    }

    @PostMapping("/return/{borrowId}")
    @Operation(summary = "归还资产")
    public R<Void> returnAsset(@PathVariable Long borrowId) {
        assetBorrowService.returnAsset(borrowId);
        return R.ok();
    }
}
