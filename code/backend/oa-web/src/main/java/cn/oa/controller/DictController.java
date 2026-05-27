package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.SysDictData;
import cn.oa.entity.SysDictType;
import cn.oa.service.DictDataService;
import cn.oa.service.DictTypeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        IPage<SysDictType> page = dictTypeService.pageList(pageNum, pageSize, dictName, dictType);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/type")
    @Operation(summary = "新增字典类型")
    @RequireAdmin
    public R<Void> addType(@RequestBody SysDictType type) {
        dictTypeService.save(type);
        return R.ok();
    }

    @PutMapping("/type")
    @Operation(summary = "修改字典类型")
    @RequireAdmin
    public R<Void> updateType(@RequestBody SysDictType type) {
        dictTypeService.updateById(type);
        return R.ok();
    }

    @DeleteMapping("/type/{id}")
    @Operation(summary = "删除字典类型")
    @RequireAdmin
    public R<Void> deleteType(@PathVariable Long id) {
        dictTypeService.removeById(id);
        return R.ok();
    }

    // ========== 字典数据 ==========

    @GetMapping("/data/page")
    @Operation(summary = "字典数据分页列表")
    public R<PageResult<SysDictData>> dataPage(@RequestParam int pageNum,
                                               @RequestParam int pageSize,
                                               @RequestParam(required = false) String dictType,
                                               @RequestParam(required = false) String dictLabel) {
        IPage<SysDictData> page = dictDataService.pageList(pageNum, pageSize, dictType, dictLabel);
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
    public R<Void> addData(@RequestBody SysDictData data) {
        dictDataService.save(data);
        return R.ok();
    }

    @PutMapping("/data")
    @Operation(summary = "修改字典数据")
    @RequireAdmin
    public R<Void> updateData(@RequestBody SysDictData data) {
        dictDataService.updateById(data);
        return R.ok();
    }

    @DeleteMapping("/data/{id}")
    @Operation(summary = "删除字典数据")
    @RequireAdmin
    public R<Void> deleteData(@PathVariable Long id) {
        dictDataService.removeById(id);
        return R.ok();
    }
}
