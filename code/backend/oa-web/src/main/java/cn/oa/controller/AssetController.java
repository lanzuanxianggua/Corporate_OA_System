package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaAsset;
import cn.oa.entity.OaAssetBorrow;
import cn.oa.service.AssetBorrowService;
import cn.oa.service.AssetService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
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
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String assetName,
                                        @RequestParam(required = false) String assetCode) {
        IPage<OaAsset> page = assetService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), category, status, assetName, assetCode);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增资产")
    @cn.oa.common.annotation.OperationLog(module = "资产管理", operation = "新增资产")
    public R<Void> add(@RequestBody @Valid OaAsset asset) {
        assetService.save(asset);
        log.info("Asset created: assetCode={}", asset.getAssetCode());
        return R.ok();
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改资产")
    @cn.oa.common.annotation.OperationLog(module = "资产管理", operation = "修改资产")
    public R<Void> update(@RequestBody @Valid OaAsset asset) {
        assetService.updateById(asset);
        log.info("Asset updated: id={}", asset.getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除资产")
    @cn.oa.common.annotation.OperationLog(module = "资产管理", operation = "删除资产")
    public R<Void> delete(@PathVariable Long id) {
        assetService.removeById(id);
        log.info("Asset deleted: id={}", id);
        return R.ok();
    }

    @PostMapping("/borrow")
    @Operation(summary = "借出资产")
    @cn.oa.common.annotation.OperationLog(module = "资产管理", operation = "借出资产")
    public R<Void> borrow(@RequestBody @Valid OaAssetBorrow borrow, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        borrow.setBorrowerId(empId);
        assetBorrowService.borrowAsset(borrow);
        log.info("Asset borrowed: assetId={}, borrowerId={}", borrow.getAssetId(), empId);
        return R.ok();
    }

    @PostMapping("/return/{borrowId}")
    @Operation(summary = "归还资产")
    @cn.oa.common.annotation.OperationLog(module = "资产管理", operation = "归还资产")
    public R<Void> returnAsset(@PathVariable Long borrowId, HttpServletRequest request) {
        Long currentEmpId = WebUtil.getEmpId(request);
        OaAssetBorrow borrow = assetBorrowService.getById(borrowId);
        if (borrow == null) {
            return R.fail("借用记录不存在");
        }
        // Only the borrower or admin can return
        if (!borrow.getBorrowerId().equals(currentEmpId) && !currentEmpId.equals(1L)) {
            return R.fail("无权归还此资产");
        }
        assetBorrowService.returnAsset(borrowId);
        log.info("Asset returned: borrowId={}", borrowId);
        return R.ok();
    }

    @GetMapping("/borrow/page")
    @Operation(summary = "分页查询借用记录")
    public R<PageResult<OaAssetBorrow>> borrowPage(
            @RequestParam int pageNum,
            @RequestParam int pageSize,
            @RequestParam(required = false) Long borrowerId,
            @RequestParam(required = false) String status) {
        IPage<OaAssetBorrow> page = assetBorrowService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), borrowerId, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}

