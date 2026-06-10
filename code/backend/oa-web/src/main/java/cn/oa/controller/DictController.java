package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.entity.SysDictData;
import cn.oa.entity.SysDictType;
import cn.oa.service.DictDataService;
import cn.oa.service.DictTypeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dict")
@Tag(name = "数据字典")
public class DictController {

    @Autowired
    private DictTypeService dictTypeService;

    @Autowired
    private DictDataService dictDataService;

    // ========== 字典类型 ==========

    @GetMapping("/type/page")
    @Operation(summary = "字典类型分页列表")
    public R<PageResult<SysDictType>> typePage(@RequestParam int pageNum,
                                               @RequestParam int pageSize,
                                               @RequestParam(required = false) String dictName,
                                               @RequestParam(required = false) String dictType) {
        IPage<SysDictType> page = dictTypeService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), dictName, dictType);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/type")
    @Operation(summary = "新增字典类型")
    @RequireAdmin
    public R<Void> addType(@RequestBody @Valid SysDictType type) {
        dictTypeService.save(type);
        log.info("Dict type created: id={}", type.getId());
        return R.ok();
    }

    @PutMapping("/type")
    @Operation(summary = "修改字典类型")
    @RequireAdmin
    public R<Void> updateType(@RequestBody @Valid SysDictType type) {
        dictTypeService.updateById(type);
        log.info("Dict type updated: id={}", type.getId());
        return R.ok();
    }

    @DeleteMapping("/type/{id}")
    @Operation(summary = "删除字典类型")
    @RequireAdmin
    public R<Void> deleteType(@PathVariable Long id) {
        dictTypeService.removeById(id);
        log.info("Dict type deleted: id={}", id);
        return R.ok();
    }

    // ========== 字典数据 ==========

    @GetMapping("/data/page")
    @Operation(summary = "字典数据分页列表")
    public R<PageResult<SysDictData>> dataPage(@RequestParam int pageNum,
                                               @RequestParam int pageSize,
                                               @RequestParam(required = false) String dictType,
                                               @RequestParam(required = false) String dictLabel) {
        IPage<SysDictData> page = dictDataService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), dictType, dictLabel);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/data/type/{dictType}")
    @Operation(summary = "根据字典类型获取数据")
    public R<List<SysDictData>> dataByType(@PathVariable String dictType) {
        return R.ok(dictDataService.getDictDataByType(dictType));
    }

    @PostMapping("/data")
    @Operation(summary = "新增字典数据")
    @RequireAdmin
    public R<Void> addData(@RequestBody @Valid SysDictData data) {
        dictDataService.save(data);
        log.info("Dict data created: id={}", data.getId());
        return R.ok();
    }

    @PutMapping("/data")
    @Operation(summary = "修改字典数据")
    @RequireAdmin
    public R<Void> updateData(@RequestBody @Valid SysDictData data) {
        dictDataService.updateById(data);
        log.info("Dict data updated: id={}", data.getId());
        return R.ok();
    }

    @DeleteMapping("/data/{id}")
    @Operation(summary = "删除字典数据")
    @RequireAdmin
    public R<Void> deleteData(@PathVariable Long id) {
        dictDataService.removeById(id);
        log.info("Dict data deleted: id={}", id);
        return R.ok();
    }
}
