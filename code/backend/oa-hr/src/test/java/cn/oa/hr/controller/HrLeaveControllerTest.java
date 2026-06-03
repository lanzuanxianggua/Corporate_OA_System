package cn.oa.hr.controller;

import cn.oa.hr.dto.HrLeaveBalanceInitDTO;
import cn.oa.hr.dto.HrLeaveCreateDTO;
import cn.oa.hr.dto.HrLeaveQueryDTO;
import cn.oa.hr.service.HrLeaveBalanceService;
import cn.oa.hr.service.HrLeaveRuleService;
import cn.oa.hr.service.HrLeaveService;
import cn.oa.hr.vo.HrLeaveBalanceVO;
import cn.oa.hr.vo.HrLeaveRuleVO;
import cn.oa.hr.vo.HrLeaveVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HR请假管理Controller测试
 * 使用MockMvcBuilders.standaloneSetup方式，不依赖SpringBootConfiguration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HR请假管理 - Controller测试")
class HrLeaveControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private HrLeaveService hrLeaveService;

    @Mock
    private HrLeaveBalanceService hrLeaveBalanceService;

    @Mock
    private HrLeaveRuleService hrLeaveRuleService;

    @InjectMocks
    private HrLeaveController hrLeaveController;

    @InjectMocks
    private HrLeaveBalanceController hrLeaveBalanceController;

    @InjectMocks
    private HrLeaveRuleController hrLeaveRuleController;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // 设置MockMvc，使用standaloneSetup避免依赖SpringBootConfiguration
        mockMvc = MockMvcBuilders.standaloneSetup(hrLeaveController, hrLeaveBalanceController, hrLeaveRuleController)
                .build();
    }

    // ==================== 请假申请测试 ====================

    @Test
    @DisplayName("创建请假申请 - 成功")
    void createAndSubmit_success() throws Exception {
        // Given
        HrLeaveCreateDTO dto = new HrLeaveCreateDTO();
        dto.setLeaveType("ANNUAL");
        dto.setStartTime(LocalDateTime.of(2026, 6, 10, 9, 0));
        dto.setEndTime(LocalDateTime.of(2026, 6, 11, 18, 0));
        dto.setLeavePeriod("FULL");
        dto.setReason("年假休息");

        when(hrLeaveService.createAndSubmit(any(HrLeaveCreateDTO.class), any(Long.class), any(Long.class)))
                .thenReturn(1L);

        // When & Then
        mockMvc.perform(post("/api/hr/leaves")
                        .requestAttr("empId", 1L)
                        .requestAttr("deptId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(1));

        verify(hrLeaveService, times(1))
                .createAndSubmit(any(HrLeaveCreateDTO.class), eq(1L), eq(10L));
    }

    @Test
    @DisplayName("创建请假申请 - 缺少必填字段")
    void createAndSubmit_missingFields() throws Exception {
        // Given - 只传leaveType，缺少其他必填字段
        String json = "{\"leaveType\":\"ANNUAL\"}";

        // When & Then
        mockMvc.perform(post("/api/hr/leaves")
                        .requestAttr("empId", 1L)
                        .requestAttr("deptId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(hrLeaveService, never()).createAndSubmit(any(), any(), any());
    }

    @Test
    @DisplayName("创建请假申请 - 未登录")
    void createAndSubmit_notLoggedIn() throws Exception {
        // Given
        HrLeaveCreateDTO dto = new HrLeaveCreateDTO();
        dto.setLeaveType("ANNUAL");
        dto.setStartTime(LocalDateTime.of(2026, 6, 10, 9, 0));
        dto.setEndTime(LocalDateTime.of(2026, 6, 11, 18, 0));
        dto.setLeavePeriod("FULL");
        dto.setReason("年假休息");

        // When & Then - 未设置empId
        mockMvc.perform(post("/api/hr/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("用户未登录"));

        verify(hrLeaveService, never()).createAndSubmit(any(), any(), any());
    }

    @Test
    @DisplayName("分页查询请假申请 - 成功")
    void pageQuery_success() throws Exception {
        // Given
        HrLeaveVO vo = new HrLeaveVO();
        vo.setId(1L);
        vo.setApplyNo("LV202606100001");
        vo.setEmpId(1L);
        vo.setEmpName("张三");
        vo.setLeaveType("ANNUAL");
        vo.setStatus("RUNNING");

        IPage<HrLeaveVO> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(vo));

        when(hrLeaveService.pageQuery(any(HrLeaveQueryDTO.class), any(Long.class), any(Boolean.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/hr/leaves")
                        .requestAttr("empId", 1L)
                        .requestAttr("isAdmin", false)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].applyNo").value("LV202606100001"));

        verify(hrLeaveService, times(1))
                .pageQuery(any(HrLeaveQueryDTO.class), eq(1L), eq(false));
    }

    @Test
    @DisplayName("查询请假详情 - 成功")
    void getDetail_success() throws Exception {
        // Given
        HrLeaveVO vo = new HrLeaveVO();
        vo.setId(1L);
        vo.setApplyNo("LV202606100001");
        vo.setEmpId(1L);
        vo.setEmpName("张三");
        vo.setLeaveType("ANNUAL");
        vo.setStatus("RUNNING");
        vo.setDays(BigDecimal.ONE);

        when(hrLeaveService.getDetail(1L)).thenReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/hr/leaves/1")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.applyNo").value("LV202606100001"));

        verify(hrLeaveService, times(1)).getDetail(1L);
    }

    @Test
    @DisplayName("查询请假详情 - 不存在")
    void getDetail_notFound() throws Exception {
        // Given
        when(hrLeaveService.getDetail(999L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/hr/leaves/999")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("请假申请不存在"));

        verify(hrLeaveService, times(1)).getDetail(999L);
    }

    @Test
    @DisplayName("撤回请假申请 - 成功")
    void revoke_success() throws Exception {
        // Given
        doNothing().when(hrLeaveService).revoke(1L, 1L, false);

        // When & Then
        mockMvc.perform(post("/api/hr/leaves/1/actions/revoke")
                        .requestAttr("empId", 1L)
                        .requestAttr("isAdmin", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(hrLeaveService, times(1)).revoke(1L, 1L, false);
    }

    @Test
    @DisplayName("撤回请假申请 - 管理员撤回")
    void revoke_byAdmin() throws Exception {
        // Given
        doNothing().when(hrLeaveService).revoke(1L, 2L, true);

        // When & Then
        mockMvc.perform(post("/api/hr/leaves/1/actions/revoke")
                        .requestAttr("empId", 2L)
                        .requestAttr("isAdmin", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(hrLeaveService, times(1)).revoke(1L, 2L, true);
    }

    @Test
    @DisplayName("重新提交请假申请 - 成功")
    void resubmit_success() throws Exception {
        // Given
        HrLeaveCreateDTO dto = new HrLeaveCreateDTO();
        dto.setLeaveType("ANNUAL");
        dto.setStartTime(LocalDateTime.of(2026, 6, 15, 9, 0));
        dto.setEndTime(LocalDateTime.of(2026, 6, 16, 18, 0));
        dto.setLeavePeriod("FULL");
        dto.setReason("年假休息");

        doNothing().when(hrLeaveService).resubmit(eq(1L), any(HrLeaveCreateDTO.class), eq(1L));

        // When & Then
        mockMvc.perform(post("/api/hr/leaves/1/actions/resubmit")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(hrLeaveService, times(1))
                .resubmit(eq(1L), any(HrLeaveCreateDTO.class), eq(1L));
    }

    @Test
    @DisplayName("查询我的余额 - 成功")
    void getMyBalances_success() throws Exception {
        // Given
        HrLeaveBalanceVO vo = new HrLeaveBalanceVO();
        vo.setEmpId(1L);
        vo.setLeaveType("ANNUAL");
        vo.setYear(2026);
        vo.setTotalDays(BigDecimal.TEN);
        vo.setUsedDays(BigDecimal.ONE);
        vo.setFrozenDays(BigDecimal.ZERO);
        vo.setRemainingDays(BigDecimal.valueOf(9));

        when(hrLeaveBalanceService.getMyBalances(1L, null))
                .thenReturn(List.of(vo));

        // When & Then
        mockMvc.perform(get("/api/hr/leaves/my-balances")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].leaveType").value("ANNUAL"))
                .andExpect(jsonPath("$.data[0].remainingDays").value(9));

        verify(hrLeaveBalanceService, times(1)).getMyBalances(1L, null);
    }

    // ==================== 假期余额管理测试 ====================

    @Test
    @DisplayName("分页查询假期余额 - 成功")
    void balancePageQuery_success() throws Exception {
        // Given
        HrLeaveBalanceVO vo = new HrLeaveBalanceVO();
        vo.setId(1L);
        vo.setEmpId(1L);
        vo.setLeaveType("ANNUAL");
        vo.setYear(2026);

        IPage<HrLeaveBalanceVO> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(vo));

        when(hrLeaveBalanceService.pageQuery(null, null, 1, 10)).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/hr/leave-balances")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(hrLeaveBalanceService, times(1)).pageQuery(null, null, 1, 10);
    }

    @Test
    @DisplayName("初始化假期余额 - 成功")
    void initBalance_success() throws Exception {
        // Given
        HrLeaveBalanceInitDTO dto = new HrLeaveBalanceInitDTO();
        dto.setEmpId(1L);
        dto.setLeaveType("ANNUAL");
        dto.setYear(2026);
        dto.setTotalDays(BigDecimal.TEN);

        doNothing().when(hrLeaveBalanceService).initBalance(any(HrLeaveBalanceInitDTO.class));

        // When & Then
        mockMvc.perform(post("/api/hr/leave-balances/actions/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(hrLeaveBalanceService, times(1)).initBalance(any(HrLeaveBalanceInitDTO.class));
    }

    @Test
    @DisplayName("初始化假期余额 - 缺少必填字段")
    void initBalance_missingFields() throws Exception {
        // Given - 缺少empId和year
        String json = "{\"leaveType\":\"ANNUAL\",\"totalDays\":10}";

        // When & Then
        mockMvc.perform(post("/api/hr/leave-balances/actions/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(hrLeaveBalanceService, never()).initBalance(any());
    }

    // ==================== 假期规则测试 ====================

    @Test
    @DisplayName("查询假期规则列表 - 成功")
    void listActiveRules_success() throws Exception {
        // Given
        HrLeaveRuleVO vo = new HrLeaveRuleVO();
        vo.setId(1L);
        vo.setLeaveType("ANNUAL");
        vo.setLeaveTypeName("年假");
        vo.setMinUnit(BigDecimal.valueOf(0.5));

        when(hrLeaveRuleService.listActiveRules()).thenReturn(List.of(vo));

        // When & Then
        mockMvc.perform(get("/api/hr/leave-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].leaveType").value("ANNUAL"));

        verify(hrLeaveRuleService, times(1)).listActiveRules();
    }
}
