package cn.oa.hr.service;

import cn.oa.hr.dto.HrLeaveCreateDTO;
import cn.oa.hr.entity.HrLeaveApply;
import cn.oa.hr.entity.HrLeaveBalance;
import cn.oa.hr.entity.HrLeaveRule;
import cn.oa.hr.enums.HrLeaveStatus;
import cn.oa.hr.mapper.HrLeaveApplyMapper;
import cn.oa.hr.service.impl.HrLeaveServiceImpl;
import cn.oa.platform.core.exception.BusinessException;
import cn.oa.workflow.core.engine.IWorkflowEngine;
import cn.oa.workflow.model.dto.StartProcessDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HR请假服务单元测试
 *
 * @author oa-hr
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HrLeaveService 请假服务测试")
class HrLeaveServiceTest {

    @Mock
    private HrLeaveApplyMapper applyMapper;

    @Mock
    private HrLeaveBalanceService balanceService;

    @Mock
    private HrLeaveRuleService ruleService;

    @Mock
    private IWorkflowEngine workflowEngine;

    @InjectMocks
    private HrLeaveServiceImpl leaveService;

    private HrLeaveCreateDTO createDTO(LocalDateTime start, LocalDateTime end,
                                        String leaveType, String period) {
        HrLeaveCreateDTO dto = new HrLeaveCreateDTO();
        dto.setLeaveType(leaveType);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setLeavePeriod(period);
        dto.setReason("休假");
        return dto;
    }

    private HrLeaveRule createRule(String leaveType, boolean deductBalance,
                                    boolean requireAttachment, BigDecimal maxDays) {
        HrLeaveRule rule = new HrLeaveRule();
        rule.setLeaveType(leaveType);
        rule.setDeductBalance(deductBalance ? 1 : 0);
        rule.setRequireAttachment(requireAttachment ? 1 : 0);
        rule.setMaxDaysPerApply(maxDays);
        rule.setMinUnit(new BigDecimal("0.5"));
        return rule;
    }

    private HrLeaveApply createPersistedApply(Long id, String status, BigDecimal days,
                                               String leaveType) {
        HrLeaveApply apply = new HrLeaveApply();
        apply.setId(id);
        apply.setEmpId(1L);
        apply.setDeptId(10L);
        apply.setLeaveType(leaveType);
        apply.setStartTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        apply.setEndTime(LocalDateTime.of(2026, 6, 1, 18, 0));
        apply.setDays(days);
        apply.setStatus(status);
        apply.setProcessInstanceId(100L);
        return apply;
    }

    // ==================== calculateLeaveDays ====================

    @Nested
    @DisplayName("请假天数计算")
    class CalculateLeaveDays {

        @Test
        @DisplayName("全天-1个工作日")
        void fullDay_oneDay() {
            BigDecimal days = leaveService.calculateLeaveDays(
                    LocalDateTime.of(2026, 6, 1, 9, 0), // Monday
                    LocalDateTime.of(2026, 6, 1, 18, 0),
                    "FULL");
            assertThat(days).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("全天-跨周跳过周末")
        void fullDay_crossWeek() {
            // Thursday to next Monday
            BigDecimal days = leaveService.calculateLeaveDays(
                    LocalDateTime.of(2026, 6, 4, 9, 0),  // Thursday
                    LocalDateTime.of(2026, 6, 8, 18, 0), // Monday
                    "FULL");
            // Thu + Fri + Mon = 3 (skip Sat/Sun)
            assertThat(days).isEqualByComparingTo(BigDecimal.valueOf(3));
        }

        @Test
        @DisplayName("半天-AM同一天")
        void halfDay_am_sameDay() {
            BigDecimal days = leaveService.calculateLeaveDays(
                    LocalDateTime.of(2026, 6, 1, 9, 0),
                    LocalDateTime.of(2026, 6, 1, 12, 0),
                    "AM");
            assertThat(days).isEqualByComparingTo(new BigDecimal("0.5"));
        }

        @Test
        @DisplayName("半天-PM同一天")
        void halfDay_pm_sameDay() {
            BigDecimal days = leaveService.calculateLeaveDays(
                    LocalDateTime.of(2026, 6, 1, 14, 0),
                    LocalDateTime.of(2026, 6, 1, 18, 0),
                    "PM");
            assertThat(days).isEqualByComparingTo(new BigDecimal("0.5"));
        }

        @Test
        @DisplayName("半天-跨天最后一天半天")
        void halfDay_crossDay() {
            BigDecimal days = leaveService.calculateLeaveDays(
                    LocalDateTime.of(2026, 6, 4, 9, 0),  // Thursday
                    LocalDateTime.of(2026, 6, 8, 12, 0), // Monday noon
                    "AM");
            // Thu(1) + Fri(1) + Mon(0.5) = 2.5
            assertThat(days).isEqualByComparingTo(new BigDecimal("2.5"));
        }

        @Test
        @DisplayName("纯周末返回0")
        void weekendOnly_zero() {
            BigDecimal days = leaveService.calculateLeaveDays(
                    LocalDateTime.of(2026, 6, 6, 9, 0),  // Saturday
                    LocalDateTime.of(2026, 6, 7, 18, 0), // Sunday
                    "FULL");
            assertThat(days).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ==================== createAndSubmit ====================

    @Nested
    @DisplayName("创建并提交请假申请")
    class CreateAndSubmit {

        @Test
        @DisplayName("正常提交1天请假-不需要扣余额")
        void submit_oneDay_noBalance() {
            HrLeaveCreateDTO dto = createDTO(
                    LocalDateTime.of(2026, 6, 1, 9, 0),
                    LocalDateTime.of(2026, 6, 1, 18, 0),
                    "PERSONAL", "FULL");

            when(ruleService.validateLeaveRequest(eq("PERSONAL"), any(BigDecimal.class), anyBoolean()))
                    .thenReturn(null);
            when(ruleService.getRuleByLeaveType("PERSONAL"))
                    .thenReturn(createRule("PERSONAL", false, false, null));
            when(applyMapper.insert(any(HrLeaveApply.class))).thenAnswer(inv -> {
                HrLeaveApply a = inv.getArgument(0);
                a.setId(1L);
                return 1;
            });
            when(workflowEngine.startWorkflow(any(StartProcessDTO.class))).thenReturn(100L);

            Long id = leaveService.createAndSubmit(dto, 1L, 10L);

            assertThat(id).isEqualTo(1L);

            ArgumentCaptor<HrLeaveApply> captor = ArgumentCaptor.forClass(HrLeaveApply.class);
            verify(applyMapper).insert(captor.capture());
            HrLeaveApply saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo("RUNNING");
            assertThat(saved.getDays()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(saved.getApplyNo()).startsWith("LV");
            assertThat(saved.getEmpId()).isEqualTo(1L);

            // 不应冻结余额
            verify(balanceService, never()).freezeBalance(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
        }

        @Test
        @DisplayName("正常提交1天请假-需要扣余额-冻结成功")
        void submit_oneDay_withBalance() {
            HrLeaveCreateDTO dto = createDTO(
                    LocalDateTime.of(2026, 6, 1, 9, 0),
                    LocalDateTime.of(2026, 6, 1, 18, 0),
                    "ANNUAL", "FULL");

            when(ruleService.validateLeaveRequest(eq("ANNUAL"), any(BigDecimal.class), anyBoolean()))
                    .thenReturn(null);
            when(ruleService.getRuleByLeaveType("ANNUAL"))
                    .thenReturn(createRule("ANNUAL", true, false, null));
            when(balanceService.freezeBalance(eq(1L), eq("ANNUAL"), eq(2026), any(BigDecimal.class)))
                    .thenReturn(true);
            when(applyMapper.insert(any(HrLeaveApply.class))).thenAnswer(inv -> {
                HrLeaveApply a = inv.getArgument(0);
                a.setId(1L);
                return 1;
            });
            when(workflowEngine.startWorkflow(any(StartProcessDTO.class))).thenReturn(100L);

            Long id = leaveService.createAndSubmit(dto, 1L, 10L);

            assertThat(id).isEqualTo(1L);
            verify(balanceService).freezeBalance(1L, "ANNUAL", 2026, BigDecimal.ONE);
        }

        @Test
        @DisplayName("余额不足-抛出异常")
        void submit_insufficientBalance() {
            HrLeaveCreateDTO dto = createDTO(
                    LocalDateTime.of(2026, 6, 1, 9, 0),
                    LocalDateTime.of(2026, 6, 1, 18, 0),
                    "ANNUAL", "FULL");

            when(ruleService.validateLeaveRequest(eq("ANNUAL"), any(BigDecimal.class), anyBoolean()))
                    .thenReturn(null);
            when(ruleService.getRuleByLeaveType("ANNUAL"))
                    .thenReturn(createRule("ANNUAL", true, false, null));
            when(balanceService.freezeBalance(eq(1L), eq("ANNUAL"), eq(2026), any(BigDecimal.class)))
                    .thenReturn(false);
            when(balanceService.getBalance(1L, "ANNUAL", 2026))
                    .thenReturn(new HrLeaveBalance());

            assertThatThrownBy(() -> leaveService.createAndSubmit(dto, 1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("假期余额不足");

            verify(applyMapper, never()).insert(any(HrLeaveApply.class));
        }

        @Test
        @DisplayName("需要附件但未上传-抛出异常")
        void submit_requireAttachment() {
            HrLeaveCreateDTO dto = createDTO(
                    LocalDateTime.of(2026, 6, 1, 9, 0),
                    LocalDateTime.of(2026, 6, 1, 18, 0),
                    "SICK", "FULL");
            // no attachments

            when(ruleService.validateLeaveRequest(eq("SICK"), any(BigDecimal.class), eq(false)))
                    .thenReturn("病假需要上传附件");

            assertThatThrownBy(() -> leaveService.createAndSubmit(dto, 1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("病假需要上传附件");
        }

        @Test
        @DisplayName("半天请假-计算0.5天")
        void submit_halfDay() {
            HrLeaveCreateDTO dto = createDTO(
                    LocalDateTime.of(2026, 6, 1, 9, 0),
                    LocalDateTime.of(2026, 6, 1, 12, 0),
                    "PERSONAL", "AM");

            when(ruleService.validateLeaveRequest(eq("PERSONAL"), any(BigDecimal.class), anyBoolean()))
                    .thenReturn(null);
            when(ruleService.getRuleByLeaveType("PERSONAL"))
                    .thenReturn(createRule("PERSONAL", false, false, null));
            when(applyMapper.insert(any(HrLeaveApply.class))).thenAnswer(inv -> {
                HrLeaveApply a = inv.getArgument(0);
                a.setId(1L);
                return 1;
            });
            when(workflowEngine.startWorkflow(any(StartProcessDTO.class))).thenReturn(100L);

            leaveService.createAndSubmit(dto, 1L, 10L);

            ArgumentCaptor<HrLeaveApply> captor = ArgumentCaptor.forClass(HrLeaveApply.class);
            verify(applyMapper).insert(captor.capture());
            assertThat(captor.getValue().getDays()).isEqualByComparingTo(new BigDecimal("0.5"));
        }

        @Test
        @DisplayName("跨周请假-跳过周末")
        void submit_crossWeek() {
            HrLeaveCreateDTO dto = createDTO(
                    LocalDateTime.of(2026, 6, 4, 9, 0),   // Thursday
                    LocalDateTime.of(2026, 6, 8, 18, 0),   // Monday
                    "PERSONAL", "FULL");

            when(ruleService.validateLeaveRequest(eq("PERSONAL"), any(BigDecimal.class), anyBoolean()))
                    .thenReturn(null);
            when(ruleService.getRuleByLeaveType("PERSONAL"))
                    .thenReturn(createRule("PERSONAL", false, false, null));
            when(applyMapper.insert(any(HrLeaveApply.class))).thenAnswer(inv -> {
                HrLeaveApply a = inv.getArgument(0);
                a.setId(1L);
                return 1;
            });
            when(workflowEngine.startWorkflow(any(StartProcessDTO.class))).thenReturn(100L);

            leaveService.createAndSubmit(dto, 1L, 10L);

            ArgumentCaptor<HrLeaveApply> captor = ArgumentCaptor.forClass(HrLeaveApply.class);
            verify(applyMapper).insert(captor.capture());
            assertThat(captor.getValue().getDays()).isEqualByComparingTo(BigDecimal.valueOf(3));
        }

        @Test
        @DisplayName("结束时间早于开始时间-抛出异常")
        void submit_endBeforeStart() {
            HrLeaveCreateDTO dto = createDTO(
                    LocalDateTime.of(2026, 6, 2, 9, 0),
                    LocalDateTime.of(2026, 6, 1, 18, 0),
                    "PERSONAL", "FULL");

            assertThatThrownBy(() -> leaveService.createAndSubmit(dto, 1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("结束时间不能早于开始时间");
        }
    }

    // ==================== Workflow Callbacks ====================

    @Nested
    @DisplayName("工作流审批回调")
    class WorkflowCallbacks {

        @Test
        @DisplayName("审批通过-状态变为PASSED-确认余额")
        void onApproved() {
            HrLeaveApply apply = createPersistedApply(1L, "RUNNING", BigDecimal.ONE, "ANNUAL");

            when(applyMapper.selectById(1L)).thenReturn(apply);
            when(ruleService.getRuleByLeaveType("ANNUAL"))
                    .thenReturn(createRule("ANNUAL", true, false, null));
            when(balanceService.confirmBalance(1L, "ANNUAL", 2026, BigDecimal.ONE))
                    .thenReturn(true);

            leaveService.onWorkflowApproved(1L, LocalDateTime.of(2026, 6, 2, 10, 0));

            assertThat(apply.getStatus()).isEqualTo("PASSED");
            assertThat(apply.getApprovedTime()).isNotNull();
            verify(balanceService).confirmBalance(1L, "ANNUAL", 2026, BigDecimal.ONE);
        }

        @Test
        @DisplayName("审批驳回-状态变为REJECTED-释放余额")
        void onRejected() {
            HrLeaveApply apply = createPersistedApply(1L, "RUNNING", BigDecimal.ONE, "ANNUAL");

            when(applyMapper.selectById(1L)).thenReturn(apply);
            when(ruleService.getRuleByLeaveType("ANNUAL"))
                    .thenReturn(createRule("ANNUAL", true, false, null));
            when(balanceService.releaseFrozenBalance(1L, "ANNUAL", 2026, BigDecimal.ONE))
                    .thenReturn(true);

            leaveService.onWorkflowRejected(1L, "请假理由不充分");

            assertThat(apply.getStatus()).isEqualTo("REJECTED");
            assertThat(apply.getRejectReason()).isEqualTo("请假理由不充分");
            verify(balanceService).releaseFrozenBalance(1L, "ANNUAL", 2026, BigDecimal.ONE);
        }

        @Test
        @DisplayName("工作流撤回回调-状态变为REVOKED-释放余额")
        void onWithdrawn() {
            HrLeaveApply apply = createPersistedApply(1L, "RUNNING", BigDecimal.ONE, "ANNUAL");

            when(applyMapper.selectById(1L)).thenReturn(apply);
            when(ruleService.getRuleByLeaveType("ANNUAL"))
                    .thenReturn(createRule("ANNUAL", true, false, null));
            when(balanceService.releaseFrozenBalance(1L, "ANNUAL", 2026, BigDecimal.ONE))
                    .thenReturn(true);

            leaveService.onWorkflowWithdrawn(1L);

            assertThat(apply.getStatus()).isEqualTo("REVOKED");
            verify(balanceService).releaseFrozenBalance(1L, "ANNUAL", 2026, BigDecimal.ONE);
        }

        @Test
        @DisplayName("重复审批通过回调-幂等不重复扣减")
        void onApproved_idempotent() {
            HrLeaveApply apply = createPersistedApply(1L, "PASSED", BigDecimal.ONE, "ANNUAL");

            when(applyMapper.selectById(1L)).thenReturn(apply);

            leaveService.onWorkflowApproved(1L, LocalDateTime.now());

            // 状态不变，不调用余额操作
            verify(balanceService, never()).confirmBalance(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
            verify(applyMapper, never()).updateById(any(HrLeaveApply.class));
        }

        @Test
        @DisplayName("重复驳回回调-幂等不重复释放")
        void onRejected_idempotent() {
            HrLeaveApply apply = createPersistedApply(1L, "REJECTED", BigDecimal.ONE, "ANNUAL");

            when(applyMapper.selectById(1L)).thenReturn(apply);

            leaveService.onWorkflowRejected(1L, "重复驳回");

            verify(balanceService, never()).releaseFrozenBalance(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
            verify(applyMapper, never()).updateById(any(HrLeaveApply.class));
        }

        @Test
        @DisplayName("审批通过-不需要扣余额-不调用余额确认")
        void onApproved_noDeductBalance() {
            HrLeaveApply apply = createPersistedApply(1L, "RUNNING", BigDecimal.ONE, "PERSONAL");

            when(applyMapper.selectById(1L)).thenReturn(apply);
            when(ruleService.getRuleByLeaveType("PERSONAL"))
                    .thenReturn(createRule("PERSONAL", false, false, null));

            leaveService.onWorkflowApproved(1L, LocalDateTime.now());

            assertThat(apply.getStatus()).isEqualTo("PASSED");
            verify(balanceService, never()).confirmBalance(anyLong(), anyString(), anyInt(), any(BigDecimal.class));
        }
    }

    // ==================== revoke ====================

    @Nested
    @DisplayName("撤回请假申请")
    class Revoke {

        @Test
        @DisplayName("正常撤回-RUNNING状态")
        void revoke_running() {
            HrLeaveApply apply = createPersistedApply(1L, "RUNNING", BigDecimal.ONE, "ANNUAL");

            when(applyMapper.selectById(1L)).thenReturn(apply);
            when(ruleService.getRuleByLeaveType("ANNUAL"))
                    .thenReturn(createRule("ANNUAL", true, false, null));
            when(balanceService.releaseFrozenBalance(1L, "ANNUAL", 2026, BigDecimal.ONE))
                    .thenReturn(true);

            leaveService.revoke(1L, 1L, false);

            assertThat(apply.getStatus()).isEqualTo("REVOKED");
            verify(balanceService).releaseFrozenBalance(1L, "ANNUAL", 2026, BigDecimal.ONE);
        }

        @Test
        @DisplayName("PASSED状态不可撤回")
        void revoke_passed_throws() {
            HrLeaveApply apply = createPersistedApply(1L, "PASSED", BigDecimal.ONE, "ANNUAL");

            when(applyMapper.selectById(1L)).thenReturn(apply);

            assertThatThrownBy(() -> leaveService.revoke(1L, 1L, false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("当前状态不允许撤回");
        }

        @Test
        @DisplayName("非申请人非管理员不可撤回")
        void revoke_notOwner_throws() {
            HrLeaveApply apply = createPersistedApply(1L, "RUNNING", BigDecimal.ONE, "ANNUAL");

            when(applyMapper.selectById(1L)).thenReturn(apply);

            assertThatThrownBy(() -> leaveService.revoke(1L, 999L, false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("只有申请人或管理员可以撤回");
        }

        @Test
        @DisplayName("管理员可撤回他人申请")
        void revoke_admin() {
            HrLeaveApply apply = createPersistedApply(1L, "RUNNING", BigDecimal.ONE, "ANNUAL");

            when(applyMapper.selectById(1L)).thenReturn(apply);
            when(ruleService.getRuleByLeaveType("ANNUAL"))
                    .thenReturn(createRule("ANNUAL", true, false, null));
            when(balanceService.releaseFrozenBalance(1L, "ANNUAL", 2026, BigDecimal.ONE))
                    .thenReturn(true);

            leaveService.revoke(1L, 999L, true);

            assertThat(apply.getStatus()).isEqualTo("REVOKED");
        }
    }

    // ==================== Balance operations ====================

    @Nested
    @DisplayName("余额冻结/确认/释放逻辑")
    class BalanceOperations {

        @Test
        @DisplayName("冻结余额成功")
        void freeze_success() {
            when(balanceService.freezeBalance(1L, "ANNUAL", 2026, BigDecimal.ONE))
                    .thenReturn(true);

            boolean result = balanceService.freezeBalance(1L, "ANNUAL", 2026, BigDecimal.ONE);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("冻结余额失败-余额不足")
        void freeze_insufficient() {
            when(balanceService.freezeBalance(1L, "ANNUAL", 2026, BigDecimal.TEN))
                    .thenReturn(false);

            boolean result = balanceService.freezeBalance(1L, "ANNUAL", 2026, BigDecimal.TEN);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("确认余额成功")
        void confirm_success() {
            when(balanceService.confirmBalance(1L, "ANNUAL", 2026, BigDecimal.ONE))
                    .thenReturn(true);

            boolean result = balanceService.confirmBalance(1L, "ANNUAL", 2026, BigDecimal.ONE);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("释放冻结余额成功")
        void release_success() {
            when(balanceService.releaseFrozenBalance(1L, "ANNUAL", 2026, BigDecimal.ONE))
                    .thenReturn(true);

            boolean result = balanceService.releaseFrozenBalance(1L, "ANNUAL", 2026, BigDecimal.ONE);
            assertThat(result).isTrue();
        }
    }

    // ==================== Rule Validation ====================

    @Nested
    @DisplayName("规则校验")
    class RuleValidation {

        @Test
        @DisplayName("无效假期类型")
        void validate_invalidType() {
            when(ruleService.validateLeaveRequest(eq("INVALID_TYPE"), any(BigDecimal.class), anyBoolean()))
                    .thenReturn("无效的假期类型: INVALID_TYPE");

            String result = ruleService.validateLeaveRequest("INVALID_TYPE", BigDecimal.ONE, false);
            assertThat(result).contains("无效的假期类型");
        }

        @Test
        @DisplayName("需要附件但未上传")
        void validate_requireAttachment() {
            when(ruleService.validateLeaveRequest(eq("SICK"), any(BigDecimal.class), eq(false)))
                    .thenReturn("病假需要上传附件");

            String result = ruleService.validateLeaveRequest("SICK", BigDecimal.ONE, false);
            assertThat(result).contains("需要上传附件");
        }

        @Test
        @DisplayName("有效请求通过校验")
        void validate_valid() {
            when(ruleService.validateLeaveRequest(eq("PERSONAL"), any(BigDecimal.class), anyBoolean()))
                    .thenReturn(null);

            String result = ruleService.validateLeaveRequest("PERSONAL", BigDecimal.ONE, false);
            assertThat(result).isNull();
        }
    }

    // Need to inject mocks for balance mapper operations

    @Test
    @DisplayName("工作流启动失败-释放冻结余额")
    void submit_workflowFail_releasesBalance() {
        HrLeaveCreateDTO dto = createDTO(
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 1, 18, 0),
                "ANNUAL", "FULL");

        when(ruleService.validateLeaveRequest(eq("ANNUAL"), any(BigDecimal.class), anyBoolean()))
                .thenReturn(null);
        when(ruleService.getRuleByLeaveType("ANNUAL"))
                .thenReturn(createRule("ANNUAL", true, false, null));
        when(balanceService.freezeBalance(eq(1L), eq("ANNUAL"), eq(2026), any(BigDecimal.class)))
                .thenReturn(true);
        when(applyMapper.insert(any(HrLeaveApply.class))).thenAnswer(inv -> {
            HrLeaveApply a = inv.getArgument(0);
            a.setId(1L);
            return 1;
        });
        when(workflowEngine.startWorkflow(any(StartProcessDTO.class)))
                .thenThrow(new RuntimeException("工作流服务不可用"));
        when(balanceService.releaseFrozenBalance(eq(1L), eq("ANNUAL"), eq(2026), any(BigDecimal.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> leaveService.createAndSubmit(dto, 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("启动审批流程失败");

        // 验证冻结的余额被释放
        verify(balanceService).releaseFrozenBalance(eq(1L), eq("ANNUAL"), eq(2026), eq(BigDecimal.ONE));
    }
}
