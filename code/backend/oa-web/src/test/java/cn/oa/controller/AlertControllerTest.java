package cn.oa.controller;

import cn.oa.entity.RptAlertLog;
import cn.oa.entity.RptAlertRule;
import cn.oa.service.AlertLogService;
import cn.oa.service.AlertRuleService;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@DisplayName("预警规则管理 - AlertController")
class AlertControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AlertRuleService alertRuleService;

    @MockitoBean
    private AlertLogService alertLogService;

    private RptAlertRule buildRule(Long id, String name) {
        RptAlertRule rule = new RptAlertRule();
        rule.setId(id);
        rule.setRuleName(name);
        rule.setRuleType("budget");
        rule.setMetric("expense_total");
        rule.setConditionType(">");
        rule.setThreshold(new BigDecimal("100000"));
        return rule;
    }

    private RptAlertLog buildLog(Long id) {
        RptAlertLog log = new RptAlertLog();
        log.setId(id);
        log.setRuleId(1L);
        log.setAlertLevel('1');
        log.setMetricValue(new BigDecimal("120000"));
        log.setThreshold(new BigDecimal("100000"));
        log.setAlertContent("经费超支预警");
        log.setHandleStatus('0');
        return log;
    }

    @Test
    @DisplayName("分页查询预警规则")
    void rulePage() throws Exception {
        IPage<RptAlertRule> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildRule(1L, "预算超支预警")));

        when(alertRuleService.pageList(1, 10, null)).thenReturn(page);

        mockMvc.perform(get("/api/alert/rule/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].ruleName").value("预算超支预警"));
    }

    @Test
    @DisplayName("新增预警规则")
    void addRule() throws Exception {
        when(alertRuleService.save(any(RptAlertRule.class))).thenReturn(true);

        mockMvc.perform(post("/api/alert/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRule(null, "新规则"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(alertRuleService, times(1)).save(any(RptAlertRule.class));
    }

    @Test
    @DisplayName("修改预警规则")
    void updateRule() throws Exception {
        when(alertRuleService.updateById(any(RptAlertRule.class))).thenReturn(true);

        mockMvc.perform(put("/api/alert/rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRule(1L, "修改后规则"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(alertRuleService, times(1)).updateById(any(RptAlertRule.class));
    }

    @Test
    @DisplayName("删除预警规则")
    void deleteRule() throws Exception {
        when(alertRuleService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/alert/rule/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(alertRuleService, times(1)).removeById(1L);
    }

    @Test
    @DisplayName("分页查询预警日志")
    void logPage() throws Exception {
        IPage<RptAlertLog> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildLog(1L)));

        when(alertLogService.pageList(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/alert/log/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("处理预警")
    void handleLog() throws Exception {
        doNothing().when(alertLogService).handle(1L, "1", "已处理");

        Map<String, String> params = Map.of("handleRemark", "已处理");

        mockMvc.perform(post("/api/alert/log/handle/1")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(alertLogService, times(1)).handle(1L, "1", "已处理");
    }
}
