package cn.oa.admin.controller;

import cn.oa.admin.dto.AdmAssetCreateDTO;
import cn.oa.admin.dto.AdmAssetOperateDTO;
import cn.oa.admin.service.AdmAssetService;
import cn.oa.admin.vo.AdmAssetLogVO;
import cn.oa.admin.vo.AdmAssetVO;
import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 固定资产管理 Controller
 *
 * @author oa-admin
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/assets")
@Tag(name = "固定资产管理")
public class AdmAssetController {

    @Autowired
    private AdmAssetService admAssetService;

    // ============ 资产基础管理 ============

    @GetMapping("/page")
    @Operation(summary = "分页查询固定资产")
    public R<PageResult<AdmAssetVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        IPage<AdmAssetVO> page = admAssetService.pageAssets(keyword, status, category, pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询资产详情")
    public R<AdmAssetVO> detail(@PathVariable Long id) {
        return R.ok(admAssetService.getAssetDetail(id));
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增固定资产")
    @OperationLog(module = "固定资产管理", operation = "新增固定资产")
    public R<Long> create(@RequestBody @Valid AdmAssetCreateDTO dto) {
        Long id = admAssetService.createAsset(dto);
        log.info("固定资产创建成功: id={}, assetCode={}", id, dto.getAssetCode());
        return R.ok(id);
    }

    @PutMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "修改固定资产")
    @OperationLog(module = "固定资产管理", operation = "修改固定资产")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid AdmAssetCreateDTO dto) {
        admAssetService.updateAsset(id, dto);
        log.info("固定资产更新成功: id={}", id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除固定资产")
    @OperationLog(module = "固定资产管理", operation = "删除固定资产")
    public R<Void> delete(@PathVariable Long id) {
        admAssetService.deleteAsset(id);
        log.info("固定资产删除成功: id={}", id);
        return R.ok();
    }

    // ============ 资产操作 ============

    @PostMapping("/operate")
    @RequireAdmin
    @Operation(summary = "资产操作（领用/归还/维修/报废）")
    @OperationLog(module = "固定资产管理", operation = "资产操作")
    public R<Void> operate(@RequestBody @Valid AdmAssetOperateDTO dto, HttpServletRequest request) {
        Long operatorId = WebUtil.getEmpId(request);
        admAssetService.operateAsset(dto, operatorId);
        log.info("资产操作成功: assetId={}, operation={}, operatorId={}", dto.getAssetId(), dto.getOperation(), operatorId);
        return R.ok();
    }

    // ============ 资产日志 ============

    @GetMapping("/log/page")
    @Operation(summary = "分页查询资产操作日志")
    public R<PageResult<AdmAssetLogVO>> logPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long assetId) {
        IPage<AdmAssetLogVO> page = admAssetService.pageAssetLogs(assetId, pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}
