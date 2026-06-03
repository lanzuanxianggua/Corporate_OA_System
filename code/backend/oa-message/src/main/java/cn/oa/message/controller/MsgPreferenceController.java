package cn.oa.message.controller;

import cn.oa.common.result.R;
import cn.oa.message.dto.MsgPreferenceDTO;
import cn.oa.message.entity.MsgUserPreference;
import cn.oa.message.service.MsgUserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "消息偏好设置")
@RestController
@RequestMapping("/api/message/preferences")
@RequiredArgsConstructor
public class MsgPreferenceController {

    private final MsgUserPreferenceService msgUserPreferenceService;

    @Operation(summary = "获取指定类型的消息偏好")
    @GetMapping("/{msgType}")
    public R<MsgUserPreference> getPreference(@RequestParam Long empId, @PathVariable String msgType) {
        return R.ok(msgUserPreferenceService.getPreference(empId, msgType));
    }

    @Operation(summary = "保存消息偏好设置")
    @PostMapping
    public R<Void> savePreference(@Valid @RequestBody MsgPreferenceDTO dto) {
        msgUserPreferenceService.savePreference(dto);
        return R.ok();
    }
}
