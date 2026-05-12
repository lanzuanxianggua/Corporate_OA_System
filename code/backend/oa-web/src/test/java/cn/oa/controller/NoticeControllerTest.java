package cn.oa.controller;

import cn.oa.entity.OaNotice;
import cn.oa.service.NoticeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoticeController.class)
@DisplayName("公告管理 - NoticeController")
class NoticeControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoticeService noticeService;

    private OaNotice buildNotice(Long id, String title) {
        OaNotice notice = new OaNotice();
        notice.setId(id);
        notice.setTitle(title);
        notice.setContent("公告内容");
        notice.setPublisherId(1L);
        notice.setCreateTime(LocalDateTime.now());
        return notice;
    }

    @Test
    @DisplayName("分页查询公告")
    void pageNotice() throws Exception {
        IPage<OaNotice> page = new Page<>(1, 10);
        page.setTotal(2);
        page.setRecords(List.of(buildNotice(1L, "放假通知"), buildNotice(2L, "系统升级")));

        when(noticeService.pageList(1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/notice/page").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].title").value("放假通知"));
    }

    @Test
    @DisplayName("获取公告详情")
    void getNoticeById() throws Exception {
        when(noticeService.getById(1L)).thenReturn(buildNotice(1L, "放假通知"));

        mockMvc.perform(get("/api/notice/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("放假通知"));
    }

    @Test
    @DisplayName("新增公告")
    void addNotice() throws Exception {
        when(noticeService.save(any(OaNotice.class))).thenReturn(true);

        mockMvc.perform(post("/api/notice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildNotice(null, "新公告"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(noticeService, times(1)).save(any(OaNotice.class));
    }

    @Test
    @DisplayName("修改公告")
    void updateNotice() throws Exception {
        when(noticeService.updateById(any(OaNotice.class))).thenReturn(true);

        mockMvc.perform(put("/api/notice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildNotice(1L, "修改后"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(noticeService, times(1)).updateById(any(OaNotice.class));
    }

    @Test
    @DisplayName("删除公告")
    void deleteNotice() throws Exception {
        when(noticeService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/notice/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(noticeService, times(1)).removeById(1L);
    }

    @Test
    @DisplayName("标记公告已读")
    void markAsRead() throws Exception {
        doNothing().when(noticeService).markAsRead(1L, 1L);

        mockMvc.perform(post("/api/notice/read/1").requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(noticeService, times(1)).markAsRead(1L, 1L);
    }
}
