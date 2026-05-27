package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaContract;
import cn.oa.service.ContractService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contract")
@Tag(name = "合同管理")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @GetMapping("/page")
    @RequireAdmin
    @Operation(summary = "分页查询合同")
    public R<PageResult<OaContract>> page(@RequestParam int pageNum,
                                            @RequestParam int pageSize,
                                            @RequestParam(required = false) String contractName,
                                            @RequestParam(required = false) String contractType,
                                            @RequestParam(required = false) String contractNo) {
        IPage<OaContract> page = contractService.pageList(pageNum, pageSize, contractName, contractType, contractNo);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增合同")
    public R<Void> add(@RequestBody OaContract contract) {
        contractService.save(contract);
        return R.ok();
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改合同")
    public R<Void> update(@RequestBody OaContract contract) {
        contractService.updateById(contract);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除合同")
    public R<Void> delete(@PathVariable Long id) {
        contractService.removeById(id);
        return R.ok();
    }

    @GetMapping("/expiring")
    @Operation(summary = "查询即将到期合同")
    public R<List<OaContract>> expiring(@RequestParam(defaultValue = "30") int days) {
        return R.ok(contractService.expiringList(days));
    }
}
