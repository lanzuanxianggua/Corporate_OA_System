package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.SysConfig;
import cn.oa.service.ConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/config")
@Tag(name = "系统参数")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @GetMapping("/page")
    @Operation(summary = "参数分页列表")
    public R<PageResult<SysConfig>> page(@RequestParam int pageNum,
                                         @RequestParam int pageSize,
                                         @RequestParam(required = false) String configName,
                                         @RequestParam(required = false) String configKey) {
        IPage<SysConfig> page = configService.pageList(pageNum, pageSize, configName, configKey);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/key/{key}")
    @Operation(summary = "根据key获取参数")
    public R<SysConfig> getByKey(@PathVariable String key) {
        return R.ok(configService.getByKey(key));
    }

    @PostMapping
    @Operation(summary = "新增参数")
    @RequireAdmin
    public R<Void> add(@RequestBody @Valid SysConfig config) {
        configService.save(config);
        log.info("Config created: key={}", config.getConfigKey());
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改参数")
    @RequireAdmin
    public R<Void> update(@RequestBody @Valid SysConfig config) {
        configService.updateById(config);
        log.info("Config updated: key={}", config.getConfigKey());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除参数")
    @RequireAdmin
    public R<Void> delete(@PathVariable Long id) {
        configService.removeById(id);
        log.info("Config deleted: id={}", id);
        return R.ok();
    }
}
