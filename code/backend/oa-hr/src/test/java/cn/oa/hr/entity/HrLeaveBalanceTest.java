package cn.oa.hr.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HR假期余额实体测试
 *
 * @author oa-hr
 */
class HrLeaveBalanceTest {

    @Test
    void testHrLeaveBalance_properties() {
        HrLeaveBalance balance = new HrLeaveBalance();

        balance.setId(1L);
        balance.setEmpId(100L);
        balance.setLeaveType("ANNUAL");
        balance.setYear(2026);
        balance.setTotalDays(new BigDecimal("15.0"));
        balance.setUsedDays(new BigDecimal("5.0"));
        balance.setFrozenDays(new BigDecimal("1.0"));
        balance.setRemainingDays(new BigDecimal("9.0"));
        balance.setExpireDate(LocalDate.of(2026, 12, 31));
        balance.setStatus("ACTIVE");

        assertEquals(1L, balance.getId());
        assertEquals(100L, balance.getEmpId());
        assertEquals("ANNUAL", balance.getLeaveType());
        assertEquals(2026, balance.getYear());
        assertEquals(new BigDecimal("15.0"), balance.getTotalDays());
        assertEquals(new BigDecimal("5.0"), balance.getUsedDays());
        assertEquals(new BigDecimal("1.0"), balance.getFrozenDays());
        assertEquals(new BigDecimal("9.0"), balance.getRemainingDays());
        assertEquals(LocalDate.of(2026, 12, 31), balance.getExpireDate());
        assertEquals("ACTIVE", balance.getStatus());
    }

    @Test
    void testHrLeaveBalance_nonDbFields() {
        HrLeaveBalance balance = new HrLeaveBalance();

        balance.setEmpName("张三");
        balance.setDeptName("研发部");
        balance.setLeaveTypeName("年假");
        balance.setAvailableDays(new BigDecimal("8.0"));

        assertEquals("张三", balance.getEmpName());
        assertEquals("研发部", balance.getDeptName());
        assertEquals("年假", balance.getLeaveTypeName());
        assertEquals(new BigDecimal("8.0"), balance.getAvailableDays());
    }
}
