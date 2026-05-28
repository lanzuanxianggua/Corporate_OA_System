package cn.oa.controller;

import cn.oa.entity.OaOuting;
import cn.oa.service.OutingService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OutingController.class)
@DisplayName("外出管理 - OutingController")
class OutingControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OutingService outingService;

    private OaOuting buildOuting(Long id) {
        OaOuting outing = new OaOuting();
        outing.setId(id);
        outing.setEmpId(1L);
        outing.setReason("客户拜访");
        outing.setDestination("上海");
        outing.setStartTime(LocalDateTime.of(2026, 6, 1, 9, 0, 0));
        outing.setEndTime(LocalDateTime.of(2026, 6, 1, 18, 0, 0));
        outing.setStatus(0);
        return outing;
    }

    @Test
    @DisplayName("提交外出申请")
    void submitOuting() throws Exception {
        doNothing().when(outingService).submit(any(OaOuting.class));

        mockMvc.perform(post("/api/outing/submit")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOuting(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(outingService, times(1)).submit(any(OaOuting.class));
    }

    @Test
    @DisplayName("审批外出申请 - 通过")
    void approveOutingPass() throws Exception {
        doNothing().when(outingService).approve(1L, 1L, 1, "同意", null);

        Map<String, Object> params = Map.of("id", 1, "status", 1, "remark", "同意");

        mockMvc.perform(post("/api/outing/approve")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(outingService, times(1)).approve(1L, 1L, 1, "同意", null);
    }

    @Test
    @DisplayName("审批外出申请 - 驳回")
    void approveOutingReject() throws Exception {
        doNothing().when(outingService).approve(1L, 1L, 2, "理由不充分", null);

        Map<String, Object> params = Map.of("id", 1, "status", 2, "remark", "理由不充分");

        mockMvc.perform(post("/api/outing/approve")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("分页查询外出申请")
    void pageOuting() throws Exception {
        IPage<OaOuting> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildOuting(1L)));

        when(outingService.pageList(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/outing/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].destination").value("上海"));
    }
}
