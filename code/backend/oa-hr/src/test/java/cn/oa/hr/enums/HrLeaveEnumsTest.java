package cn.oa.hr.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HR请假枚举测试
 *
 * @author oa-hr
 */
class HrLeaveEnumsTest {

    @Test
    void testHrLeaveType_fromCode() {
        assertEquals(HrLeaveType.ANNUAL, HrLeaveType.fromCode("ANNUAL"));
        assertEquals(HrLeaveType.SICK, HrLeaveType.fromCode("SICK"));
        assertNull(HrLeaveType.fromCode("INVALID"));
        assertNull(HrLeaveType.fromCode(null));
    }

    @Test
    void testHrLeaveType_properties() {
        assertEquals("ANNUAL", HrLeaveType.ANNUAL.getCode());
        assertEquals("年假", HrLeaveType.ANNUAL.getName());
    }

    @Test
    void testHrLeaveStatus_fromCode() {
        assertEquals(HrLeaveStatus.RUNNING, HrLeaveStatus.fromCode("RUNNING"));
        assertEquals(HrLeaveStatus.PASSED, HrLeaveStatus.fromCode("PASSED"));
        assertNull(HrLeaveStatus.fromCode("INVALID"));
    }

    @Test
    void testHrLeaveStatus_canRevoke() {
        assertTrue(HrLeaveStatus.RUNNING.canRevoke());
        assertFalse(HrLeaveStatus.PASSED.canRevoke());
        assertFalse(HrLeaveStatus.REJECTED.canRevoke());
    }

    @Test
    void testHrLeaveStatus_canResubmit() {
        assertTrue(HrLeaveStatus.REJECTED.canResubmit());
        assertFalse(HrLeaveStatus.PASSED.canResubmit());
        assertFalse(HrLeaveStatus.RUNNING.canResubmit());
    }

    @Test
    void testHrLeaveStatus_isFinal() {
        assertTrue(HrLeaveStatus.PASSED.isFinal());
        assertTrue(HrLeaveStatus.REJECTED.isFinal());
        assertTrue(HrLeaveStatus.REVOKED.isFinal());
        assertFalse(HrLeaveStatus.RUNNING.isFinal());
        assertFalse(HrLeaveStatus.DRAFT.isFinal());
    }

    @Test
    void testHrLeavePeriod_fromCode() {
        assertEquals(HrLeavePeriod.FULL, HrLeavePeriod.fromCode("FULL"));
        assertEquals(HrLeavePeriod.AM, HrLeavePeriod.fromCode("AM"));
        assertEquals(HrLeavePeriod.PM, HrLeavePeriod.fromCode("PM"));
        assertNull(HrLeavePeriod.fromCode("INVALID"));
    }

    @Test
    void testHrLeavePeriod_daysMultiplier() {
        assertEquals(1.0, HrLeavePeriod.FULL.getDaysMultiplier());
        assertEquals(0.5, HrLeavePeriod.AM.getDaysMultiplier());
        assertEquals(0.5, HrLeavePeriod.PM.getDaysMultiplier());
    }
}
