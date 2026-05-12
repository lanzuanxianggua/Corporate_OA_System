package cn.oa.controller;

import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaNotice;
import cn.oa.service.NoticeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/notice")
@Tag(name = "公告管理")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询公告")
    public R<PageResult<OaNotice>> page(@RequestParam int pageNum,
                                        @RequestParam int pageSize,
                                        HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        IPage<OaNotice> page = noticeService.pageList(pageNum, pageSize);
        // 填充 isRead 状态
        for (OaNotice notice : page.getRecords()) {
            notice.setIsRead(noticeService.isRead(notice.getId(), empId));
        }
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情")
    public R<OaNotice> getById(@PathVariable Long id) {
        OaNotice notice = noticeService.getById(id);
        return R.ok(notice);
    }

    @PostMapping
    @Operation(summary = "新增公告")
    public R<Void> add(@RequestBody OaNotice notice, HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        notice.setPublisherId(empId);
        noticeService.save(notice);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改公告")
    public R<Void> update(@RequestBody OaNotice notice) {
        noticeService.updateById(notice);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除公告")
    public R<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return R.ok();
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记公告已读")
    public R<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        noticeService.markAsRead(id, empId);
        return R.ok();
    }
}
