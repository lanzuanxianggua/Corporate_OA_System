package cn.oa.controller;

import cn.oa.entity.OaLeaveBalance;
import cn.oa.service.LeaveBalanceService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveBalanceController.class)
@DisplayName("假期余额管理 - LeaveBalanceController")
class LeaveBalanceControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveBalanceService leaveBalanceService;

    @Test
    @DisplayName("查询当前用户假期余额")
    void myBalances() throws Exception {
        OaLeaveBalance balance = new OaLeaveBalance();
        balance.setId(1L);
        balance.setEmpId(1L);
        balance.setLeaveType(1);
        balance.setYear(2026);
        balance.setTotalDays(new BigDecimal("10"));
        balance.setUsedDays(new BigDecimal("2"));
        balance.setRemainingDays(new BigDecimal("8"));

        when(leaveBalanceService.myBalances(1L)).thenReturn(List.of(balance));

        mockMvc.perform(get("/api/leave-balance/my")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].remainingDays").value(8));
    }

    @Test
    @DisplayName("初始化员工年度假期余额")
    void initYearBalance() throws Exception {
        doNothing().when(leaveBalanceService).initYearBalance(1L, 2026);

        Map<String, Object> params = Map.of("empId", 1, "year", 2026);

        mockMvc.perform(post("/api/leave-balance/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(leaveBalanceService, times(1)).initYearBalance(1L, 2026);
    }
}
