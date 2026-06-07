package cn.oa.message.controller;

import cn.oa.message.entity.MsgNotificationType;
import cn.oa.message.service.MsgNotificationTypeService;
import cn.oa.message.vo.MsgNotificationTypeVO;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.security.annotation.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知类型字典 Controller.
 *
 * <p>路径前缀: /api/v1/msg/notification-types
 */
@Tag(name = "消息通知类型")
@RestController
@RequestMapping("/api/v1/msg/notification-types")
@RequiredArgsConstructor
public class MsgNotificationTypeController {

    private final MsgNotificationTypeService typeService;

    @Operation(summary = "查询启用的类型 (前端下拉用)")
    @GetMapping("/enabled")
    @RequirePermission("msg:type:list")
    public R<List<MsgNotificationTypeVO>> listEnabled() {
        return R.ok(typeService.listEnabled());
    }

    @Operation(summary = "查询所有类型, 含禁用 (管理用)")
    @GetMapping
    @RequirePermission("msg:type:list")
    public R<List<MsgNotificationTypeVO>> listAll() {
        return R.ok(typeService.listAll());
    }

    @Operation(summary = "新增类型")
    @PostMapping
    @RequirePermission("msg:type:create")
    public R<MsgNotificationType> create(@RequestBody @Valid MsgNotificationType entity) {
        UserContext.UserInfo user = UserContext.get();
        if (user != null) {
            entity.setCreateBy(user.getUsername());
        }
        return R.ok(typeService.create(entity));
    }

    @Operation(summary = "更新类型")
    @PutMapping("/{id}")
    @RequirePermission("msg:type:update")
    public R<MsgNotificationType> update(@PathVariable Long id,
                                          @RequestBody MsgNotificationType patch) {
        return R.ok(typeService.update(id, patch));
    }

    @Operation(summary = "删除类型 (逻辑删)")
    @DeleteMapping("/{id}")
    @RequirePermission("msg:type:delete")
    public R<Void> delete(@PathVariable Long id) {
        typeService.delete(id);
        return R.ok();
    }
}
