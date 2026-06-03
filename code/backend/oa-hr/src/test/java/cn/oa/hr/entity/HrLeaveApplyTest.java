package cn.oa.hr.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HR请假实体测试
 *
 * @author oa-hr
 */
class HrLeaveApplyTest {

    @Test
    void testHrLeaveApply_properties() {
        HrLeaveApply apply = new HrLeaveApply();

        apply.setId(1L);
        apply.setApplyNo("LV202606020001");
        apply.setEmpId(100L);
        apply.setDeptId(10L);
        apply.setLeaveType("ANNUAL");
        apply.setStartTime(LocalDateTime.of(2026, 6, 3, 9, 0));
        apply.setEndTime(LocalDateTime.of(2026, 6, 3, 18, 0));
        apply.setLeavePeriod("FULL");
        apply.setDays(new BigDecimal("1.0"));
        apply.setReason("个人事务");
        apply.setStatus("DRAFT");

        assertEquals(1L, apply.getId());
        assertEquals("LV202606020001", apply.getApplyNo());
        assertEquals(100L, apply.getEmpId());
        assertEquals(10L, apply.getDeptId());
        assertEquals("ANNUAL", apply.getLeaveType());
        assertEquals("FULL", apply.getLeavePeriod());
        assertEquals(new BigDecimal("1.0"), apply.getDays());
        assertEquals("个人事务", apply.getReason());
        assertEquals("DRAFT", apply.getStatus());
    }

    @Test
    void testHrLeaveApply_nonDbFields() {
        HrLeaveApply apply = new HrLeaveApply();

        apply.setEmpName("张三");
        apply.setDeptName("研发部");
        apply.setLeaveTypeName("年假");
        apply.setStatusName("草稿");

        assertEquals("张三", apply.getEmpName());
        assertEquals("研发部", apply.getDeptName());
        assertEquals("年假", apply.getLeaveTypeName());
        assertEquals("草稿", apply.getStatusName());
    }
}
