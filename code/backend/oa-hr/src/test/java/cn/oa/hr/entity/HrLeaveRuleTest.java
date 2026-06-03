package cn.oa.hr.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HR假期规则实体测试
 *
 * @author oa-hr
 */
class HrLeaveRuleTest {

    @Test
    void testHrLeaveRule_properties() {
        HrLeaveRule rule = new HrLeaveRule();

        rule.setId(1L);
        rule.setRuleName("年假规则");
        rule.setLeaveType("ANNUAL");
        rule.setMinUnit(new BigDecimal("0.5"));
        rule.setMaxDaysPerApply(new BigDecimal("15.0"));
        rule.setDeductBalance(1);
        rule.setDeductSalary(0);
        rule.setRequireAttachment(0);
        rule.setRuleScript("days <= totalDays");
        rule.setStatus("ACTIVE");

        assertEquals(1L, rule.getId());
        assertEquals("年假规则", rule.getRuleName());
        assertEquals("ANNUAL", rule.getLeaveType());
        assertEquals(new BigDecimal("0.5"), rule.getMinUnit());
        assertEquals(new BigDecimal("15.0"), rule.getMaxDaysPerApply());
        assertEquals(1, rule.getDeductBalance());
        assertEquals(0, rule.getDeductSalary());
        assertEquals(0, rule.getRequireAttachment());
        assertEquals("days <= totalDays", rule.getRuleScript());
        assertEquals("ACTIVE", rule.getStatus());
    }
}
