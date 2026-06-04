package cn.oa.platform.web.controller;

import cn.oa.platform.common.api.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查 Controller.
 */
@Tag(name = "系统", description = "健康检查/版本/时间")
@RestController
@RequestMapping("/api")
public class PingController {

    @Operation(summary = "健康检查")
    @GetMapping("/ping")
    public R<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "oa-system");
        data.put("version", "2.0.0-SNAPSHOT");
        data.put("timestamp", Instant.now().toEpochMilli());
        return R.ok(data);
    }
}
