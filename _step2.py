from pathlib import Path
ROOT = Path(r"E:/JavaProject/Corporate_OA_System")

# AttendanceSchedule DTO in oa-model or common - put in oa-common
write(ROOT / "code/backend/oa-common/src/main/java/cn/oa/common/dto/AttendanceSchedule.java", """package cn.oa.common.dto;

import java.time.LocalTime;

public class AttendanceSchedule {

    private final LocalTime workStart;
    private final LocalTime workEnd;
    private final int lateThresholdMinutes;

    public AttendanceSchedule(LocalTime workStart, LocalTime workEnd, int lateThresholdMinutes) {
        this.workStart = workStart;
        this.workEnd = workEnd;
        this.lateThresholdMinutes = lateThresholdMinutes;
    }

    public static AttendanceSchedule defaultSchedule() {
        return new AttendanceSchedule(LocalTime.of(9, 0), LocalTime.of(18, 0), 0);
    }

    public LocalTime getWorkStart() {
        return workStart;
    }

    public LocalTime getWorkEnd() {
        return workEnd;
    }

    public int getLateThresholdMinutes() {
        return lateThresholdMinutes;
    }

    public LocalTime getLateDeadline() {
        return workStart.plusMinutes(Math.max(lateThresholdMinutes, 0));
    }
}
""")

# AttendanceGroupService interface update
write(ROOT / "code/backend/oa-service/src/main/java/cn/oa/service/AttendanceGroupService.java", """package cn.oa.service;

import cn.oa.common.dto.AttendanceSchedule;
import cn.oa.entity.OaAttendanceGroup;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AttendanceGroupService extends IService<OaAttendanceGroup> {

    IPage<OaAttendanceGroup> pageList(int pageNum, int pageSize, String groupName);

    void assignEmployees(Long groupId, List<Long> empIds);

    void removeEmployees(Long groupId, List<Long> empIds);

    AttendanceSchedule getScheduleForEmployee(Long empId);
}
""")

# AttendanceGroupServiceImpl - append method
p = ROOT / "code/backend/oa-service/src/main/java/cn/oa/service/impl/AttendanceGroupServiceImpl.java"
text = p.read_text(encoding="utf-8")
if "getScheduleForEmployee" not in text:
    text = text.replace(
        "import cn.oa.common.exception.BusinessException;",
        "import cn.oa.common.dto.AttendanceSchedule;\nimport cn.oa.common.exception.BusinessException;"
    )
    text = text.replace(
        "import java.util.List;",
        "import java.time.LocalTime;\nimport java.util.List;"
    )
    method = """
    @Override
    public AttendanceSchedule getScheduleForEmployee(Long empId) {
        if (empId == null) {
            return AttendanceSchedule.defaultSchedule();
        }
        OaAttendanceGroupEmp relation = groupEmpMapper.selectOne(
                new LambdaQueryWrapper<OaAttendanceGroupEmp>()
                        .eq(OaAttendanceGroupEmp::getEmpId, empId)
                        .orderByDesc(OaAttendanceGroupEmp::getId)
                        .last("LIMIT 1"));
        if (relation == null || relation.getGroupId() == null) {
            return AttendanceSchedule.defaultSchedule();
        }
        OaAttendanceGroup group = this.getById(relation.getGroupId());
        if (group == null || group.getStatus() == null || group.getStatus() != '0') {
            return AttendanceSchedule.defaultSchedule();
        }
        LocalTime workStart = group.getWorkStart() != null ? group.getWorkStart() : LocalTime.of(9, 0);
        LocalTime workEnd = group.getWorkEnd() != null ? group.getWorkEnd() : LocalTime.of(18, 0);
        int lateThreshold = group.getLateThreshold() != null ? group.getLateThreshold() : 0;
        return new AttendanceSchedule(workStart, workEnd, lateThreshold);
    }
"""
    text = text.rstrip() + "\n" + method + "}\n"
    p.write_text(text, encoding="utf-8")

# OaMeetingRoomMapper lock method
p = ROOT / "code/backend/oa-mapper/src/main/java/cn/oa/mapper/OaMeetingRoomMapper.java"
text = p.read_text(encoding="utf-8")
if "lockById" not in text:
    text = text.replace(
        "import org.apache.ibatis.annotations.Mapper;",
        "import org.apache.ibatis.annotations.Mapper;\nimport org.apache.ibatis.annotations.Param;\nimport org.apache.ibatis.annotations.Select;"
    )
    text = text.replace(
        "public interface OaMeetingRoomMapper extends BaseMapper<OaMeetingRoom> {\n}",
        """public interface OaMeetingRoomMapper extends BaseMapper<OaMeetingRoom> {

    @Select("SELECT id FROM oa_meeting_room WHERE id = #{roomId} FOR UPDATE")
    Long lockById(@Param("roomId") Long roomId);
}
"""
    )
    p.write_text(text, encoding="utf-8")

print("batch1 done")
