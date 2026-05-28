package cn.oa.controller;

import cn.oa.common.result.PageResult;
import cn.oa.entity.OaOperationLog;
import cn.oa.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperationLogController.class)
@DisplayName("操作日志 - OperationLogController")
class OperationLogControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OperationLogService operationLogService;

    @Test
    @DisplayName("分页查询操作日志")
    void pageLog() throws Exception {
        OaOperationLog log = new OaOperationLog();
        log.setId(1L);
        log.setModule("员工管理");
        log.setOperation("新增员工");
        log.setEmpId(1L);
        log.setCreateTime(LocalDateTime.of(2026, 5, 27, 10, 0, 0));

        PageResult<OaOperationLog> pageResult = new PageResult<>(1L, List.of(log));

        when(operationLogService.pageList(1, 10, null, null, null)).thenReturn(pageResult);

        mockMvc.perform(get("/api/operation-log/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].module").value("员工管理"));
    }

    @Test
    @DisplayName("分页查询操作日志 - 带模块过滤")
    void pageLogWithModule() throws Exception {
        PageResult<OaOperationLog> pageResult = new PageResult<>(0L, List.of());

        when(operationLogService.pageList(1, 10, "考勤管理", null, null)).thenReturn(pageResult);

        mockMvc.perform(get("/api/operation-log/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("module", "考勤管理"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
