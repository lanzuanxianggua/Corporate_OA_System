package cn.oa.service.support;

import cn.oa.common.dto.AttendanceSchedule;
import cn.oa.service.impl.AttendanceGroupServiceImpl;

/** Test stub: default schedule without DB. */
public class TestAttendanceGroupService extends AttendanceGroupServiceImpl {
    @Override
    public AttendanceSchedule getScheduleForEmployee(Long empId) {
        return AttendanceSchedule.defaultSchedule();
    }
}
