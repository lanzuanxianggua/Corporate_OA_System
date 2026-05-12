package cn.oa.controller;

import cn.oa.entity.OaLeaveApply;
import cn.oa.service.LeaveApplyService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveApplyController.class)
@DisplayName("请假管理 - LeaveApplyController")
class LeaveApplyControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveApplyService leaveApplyService;

    private OaLeaveApply buildApply(Long id) {
        OaLeaveApply apply = new OaLeaveApply();
        apply.setId(id);
        apply.setEmpId(1L);
        apply.setLeaveType(1);
        apply.setStartTime(LocalDateTime.of(2026, 5, 10, 9, 0, 0));
        apply.setEndTime(LocalDateTime.of(2026, 5, 11, 18, 0, 0));
        apply.setReason("家里有事");
        apply.setStatus(0);
        return apply;
    }

    @Test
    @DisplayName("提交请假申请")
    void submitLeave() throws Exception {
        doNothing().when(leaveApplyService).submit(any(OaLeaveApply.class));

        mockMvc.perform(post("/api/leave/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildApply(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(leaveApplyService, times(1)).submit(any(OaLeaveApply.class));
    }

    @Test
    @DisplayName("审批请假申请 - 通过")
    void approveLeavePass() throws Exception {
        doNothing().when(leaveApplyService).approve(1L, 2L, 1, "同意");

        mockMvc.perform(post("/api/leave/approve")
                        .param("applyId", "1").param("approverId", "2")
                        .param("status", "1").param("remark", "同意"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(leaveApplyService, times(1)).approve(1L, 2L, 1, "同意");
    }

    @Test
    @DisplayName("审批请假申请 - 驳回")
    void approveLeaveReject() throws Exception {
        doNothing().when(leaveApplyService).approve(1L, 2L, 2, "理由不充分");

        mockMvc.perform(post("/api/leave/approve")
                        .param("applyId", "1").param("approverId", "2")
                        .param("status", "2").param("remark", "理由不充分"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("分页查询请假申请")
    void pageLeave() throws Exception {
        IPage<OaLeaveApply> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildApply(1L)));

        when(leaveApplyService.pageList(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/leave/page").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].leaveType").value(1));
    }
}
