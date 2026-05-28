package cn.oa.controller;

import cn.oa.entity.OaOvertime;
import cn.oa.service.OvertimeService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OvertimeController.class)
@DisplayName("加班管理 - OvertimeController")
class OvertimeControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OvertimeService overtimeService;

    private OaOvertime buildOvertime(Long id) {
        OaOvertime overtime = new OaOvertime();
        overtime.setId(id);
        overtime.setEmpId(1L);
        overtime.setOvertimeDate(LocalDate.of(2026, 5, 30));
        overtime.setStartTime(LocalDateTime.of(2026, 5, 30, 19, 0, 0));
        overtime.setEndTime(LocalDateTime.of(2026, 5, 30, 22, 0, 0));
        overtime.setHours(new BigDecimal("3.0"));
        overtime.setReason("项目上线");
        overtime.setStatus("0");
        return overtime;
    }

    @Test
    @DisplayName("提交加班申请")
    void submitOvertime() throws Exception {
        doNothing().when(overtimeService).submit(any(OaOvertime.class));

        mockMvc.perform(post("/api/overtime/submit")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOvertime(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(overtimeService, times(1)).submit(any(OaOvertime.class));
    }

    @Test
    @DisplayName("审批加班申请 - 通过")
    void approveOvertimePass() throws Exception {
        doNothing().when(overtimeService).approve(1L, 1L, 1, "同意");

        Map<String, Object> params = Map.of("id", 1, "status", 1, "remark", "同意");

        mockMvc.perform(post("/api/overtime/approve")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(overtimeService, times(1)).approve(1L, 1L, 1, "同意");
    }

    @Test
    @DisplayName("分页查询加班申请")
    void pageOvertime() throws Exception {
        IPage<OaOvertime> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildOvertime(1L)));

        when(overtimeService.pageList(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/overtime/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].reason").value("项目上线"));
    }
}
