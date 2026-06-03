package cn.oa.document.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.document.dto.DocReceiveApproveDTO;
import cn.oa.document.dto.DocReceiveCreateDTO;
import cn.oa.document.dto.DocReceiveHandleDTO;
import cn.oa.document.dto.DocReceiveProposeDTO;
import cn.oa.document.service.DocReceiveService;
import cn.oa.document.vo.DocReceiveVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 收文管理控制器
 *
 * @author oa-document
 */
@Slf4j
@RestController
@RequestMapping("/api/document/receive")
@Tag(name = "收文管理")
@RequiredArgsConstructor
public class DocReceiveController {

    private final DocReceiveService docReceiveService;

    @PostMapping
    @Operation(summary = "登记收文")
    @OperationLog(module = "收文管理", operation = "登记收文")
    public R<Long> register(@RequestBody @Valid DocReceiveCreateDTO dto, HttpServletRequest request) {
        String creator = WebUtil.getEmpName(request);
        Long id = docReceiveService.register(dto, creator);
        log.info("收文登记成功: id={}", id);
        return R.ok(id);
    }

    @PostMapping("/propose")
    @Operation(summary = "拟办")
    @OperationLog(module = "收文管理", operation = "拟办")
    public R<Void> propose(@RequestBody @Valid DocReceiveProposeDTO dto) {
        docReceiveService.propose(dto);
        return R.ok();
    }

    @PostMapping("/approve")
    @Operation(summary = "批办")
    @OperationLog(module = "收文管理", operation = "批办")
    public R<Void> approve(@RequestBody @Valid DocReceiveApproveDTO dto) {
        docReceiveService.approve(dto);
        return R.ok();
    }

    @PostMapping("/handle")
    @Operation(summary = "承办")
    @OperationLog(module = "收文管理", operation = "承办")
    public R<Void> handle(@RequestBody @Valid DocReceiveHandleDTO dto) {
        docReceiveService.handle(dto);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询收文")
    public R<PageResult<DocReceiveVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        IPage<DocReceiveVO> page = docReceiveService.pageReceive(pageNum, pageSize, keyword, status);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询收文详情")
    public R<DocReceiveVO> detail(@PathVariable Long id) {
        return R.ok(docReceiveService.getReceiveDetail(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除收文")
    @OperationLog(module = "收文管理", operation = "删除收文")
    public R<Void> delete(@PathVariable Long id) {
        docReceiveService.deleteReceive(id);
        return R.ok();
    }
}
