package cn.oa.service.impl;

import cn.oa.common.dto.AttendanceSchedule;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaAttendanceGroup;
import cn.oa.entity.OaAttendanceGroupEmp;
import cn.oa.mapper.OaAttendanceGroupEmpMapper;
import cn.oa.mapper.OaAttendanceGroupMapper;
import cn.oa.service.AttendanceGroupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceGroupServiceImpl extends ServiceImpl<OaAttendanceGroupMapper, OaAttendanceGroup> implements AttendanceGroupService {

    @Autowired
    private OaAttendanceGroupEmpMapper groupEmpMapper;

    @Override
    public IPage<OaAttendanceGroup> pageList(int pageNum, int pageSize, String groupName) {
        Page<OaAttendanceGroup> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaAttendanceGroup> wrapper = new LambdaQueryWrapper<>();
        if (groupName != null && !groupName.isEmpty()) {
            wrapper.like(OaAttendanceGroup::getGroupName, groupName);
        }
        wrapper.orderByDesc(OaAttendanceGroup::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional
    public void assignEmployees(Long groupId, List<Long> empIds) {
        OaAttendanceGroup group = this.getById(groupId);
        if (group == null) {
            throw new BusinessException("??????");
        }
        List<OaAttendanceGroupEmp> existing = groupEmpMapper.selectList(
                new LambdaQueryWrapper<OaAttendanceGroupEmp>().eq(OaAttendanceGroupEmp::getGroupId, groupId));
        List<Long> existingEmpIds = existing.stream().map(OaAttendanceGroupEmp::getEmpId).collect(Collectors.toList());

        for (Long empId : empIds) {
            if (!existingEmpIds.contains(empId)) {
                groupEmpMapper.delete(new LambdaQueryWrapper<OaAttendanceGroupEmp>().eq(OaAttendanceGroupEmp::getEmpId, empId));
                OaAttendanceGroupEmp ge = new OaAttendanceGroupEmp();
                ge.setGroupId(groupId);
                ge.setEmpId(empId);
                groupEmpMapper.insert(ge);
            }
        }
    }

    @Override
    @Transactional
    public void removeEmployees(Long groupId, List<Long> empIds) {
        if (empIds == null || empIds.isEmpty()) {
            throw new BusinessException("????????");
        }
        groupEmpMapper.delete(
                new LambdaQueryWrapper<OaAttendanceGroupEmp>()
                        .eq(OaAttendanceGroupEmp::getGroupId, groupId)
                        .in(OaAttendanceGroupEmp::getEmpId, empIds));
    }

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
}
