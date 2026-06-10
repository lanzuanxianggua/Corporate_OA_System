package cn.oa.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaNotice;
import cn.oa.entity.dto.NoticeDTO;
import cn.oa.service.NoticeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/notice")
@Tag(name = "公告管理")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询公告")
    public R<cn.oa.common.result.PageResult<OaNotice>> page(@RequestParam int pageNum,
                                        @RequestParam int pageSize,
                                        @RequestParam(required = false) String title,
                                        HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        IPage<OaNotice> page = noticeService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), title);
        for (OaNotice notice : page.getRecords()) {
            notice.setIsRead(noticeService.isRead(notice.getId(), empId));
        }
        return R.ok(cn.oa.common.result.PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情")
    public R<OaNotice> getById(@PathVariable Long id) {
        OaNotice notice = noticeService.getById(id);
        if (notice == null) {
            return R.fail("公告不存在");
        }
        return R.ok(notice);
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增公告")
    @OperationLog(module = "公告管理", operation = "新增公告")
    public R<Void> add(@RequestBody @Valid NoticeDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        OaNotice notice = new OaNotice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setNoticeType(dto.getNoticeType());
        notice.setPublisherId(empId);
        noticeService.save(notice);
        log.info("Notice created: id={}, publisherId={}", notice.getId(), empId);
        return R.ok();
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改公告")
    @OperationLog(module = "公告管理", operation = "修改公告")
    public R<Void> update(@RequestBody @Valid NoticeDTO dto) {
        OaNotice notice = new OaNotice();
        notice.setId(dto.getId());
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setNoticeType(dto.getNoticeType());
        notice.setStatus(dto.getStatus());
        noticeService.updateById(notice);
        log.info("Notice updated: id={}", dto.getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除公告")
    @OperationLog(module = "公告管理", operation = "删除公告")
    public R<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        log.info("Notice deleted: id={}", id);
        return R.ok();
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记公告已读")
    public R<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        noticeService.markAsRead(id, empId);
        return R.ok();
    }
}
