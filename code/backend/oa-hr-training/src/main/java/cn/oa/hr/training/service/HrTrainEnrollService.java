package cn.oa.hr.training.service;

import cn.oa.hr.training.entity.HrTrainCourse;
import cn.oa.hr.training.entity.HrTrainEnroll;
import cn.oa.hr.training.entity.HrTrainPlan;
import cn.oa.hr.training.entity.HrTrainRecord;
import cn.oa.hr.training.entity.HrTrainSession;
import cn.oa.hr.training.mapper.HrTrainCourseMapper;
import cn.oa.hr.training.mapper.HrTrainEnrollMapper;
import cn.oa.hr.training.mapper.HrTrainPlanMapper;
import cn.oa.hr.training.mapper.HrTrainRecordMapper;
import cn.oa.hr.training.mapper.HrTrainSessionMapper;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HrTrainEnrollService {
    private final HrTrainEnrollMapper mapper;
    private final HrTrainSessionMapper sessionMapper;
    private final HrTrainPlanMapper planMapper;
    private final HrTrainCourseMapper courseMapper;
    private final HrTrainRecordMapper recordMapper;

    @Transactional
    public Long enroll(Long sessionId, Long empId) {
        HrTrainSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BizException(RCode.NOT_FOUND, "Training session not found");
        int enrolled = session.getEnrolledNum() == null ? 0 : session.getEnrolledNum();
        int capacity = session.getMaxCapacity() == null ? 0 : session.getMaxCapacity();
        if (capacity > 0 && enrolled >= capacity) throw new BizException(RCode.BAD_REQUEST, "Training session is full");

        HrTrainEnroll enroll = new HrTrainEnroll();
        enroll.setSessionId(sessionId);
        enroll.setEmpId(empId);
        enroll.setEnrollTime(LocalDateTime.now());
        enroll.setAttendance("PENDING");
        mapper.insert(enroll);

        session.setEnrolledNum(enrolled + 1);
        sessionMapper.updateById(session);
        return enroll.getId();
    }

    @Transactional
    public void signIn(Long id) {
        HrTrainEnroll enroll = mapper.selectById(id);
        enroll.setAttendance("SIGNED");
        enroll.setSignTime(LocalDateTime.now());
        mapper.updateById(enroll);
    }

    @Transactional
    public void score(Long id, BigDecimal score) {
        HrTrainEnroll enroll = mapper.selectById(id);
        enroll.setScore(score);
        HrTrainSession session = sessionMapper.selectById(enroll.getSessionId());
        HrTrainPlan plan = session == null ? null : planMapper.selectById(session.getPlanId());
        HrTrainCourse course = plan == null ? null : courseMapper.selectById(plan.getCourseId());
        BigDecimal credit = course == null || course.getCredit() == null ? BigDecimal.ZERO : course.getCredit();
        enroll.setCreditGranted(credit);
        mapper.updateById(enroll);

        HrTrainRecord record = new HrTrainRecord();
        record.setEmpId(enroll.getEmpId());
        record.setSessionId(enroll.getSessionId());
        record.setCourseId(plan == null ? null : plan.getCourseId());
        record.setTotalCredit(credit);
        recordMapper.insert(record);
    }

    public Page<HrTrainEnroll> listPage(Long sessionId, Long empId, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrTrainEnroll>()
                .eq(sessionId != null, HrTrainEnroll::getSessionId, sessionId)
                .eq(empId != null, HrTrainEnroll::getEmpId, empId)
                .orderByDesc(HrTrainEnroll::getEnrollTime));
    }

    public Page<HrTrainRecord> listRecords(Long empId, int pn, int ps) {
        return recordMapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrTrainRecord>()
                .eq(empId != null, HrTrainRecord::getEmpId, empId)
                .orderByDesc(HrTrainRecord::getCreateTime));
    }
}
