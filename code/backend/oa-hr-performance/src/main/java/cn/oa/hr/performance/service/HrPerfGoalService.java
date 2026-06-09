package cn.oa.hr.performance.service;

import cn.oa.hr.performance.entity.HrPerfGoal;
import cn.oa.hr.performance.mapper.HrPerfGoalMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrPerfGoalService {
    private final HrPerfGoalMapper mapper;

    @Transactional
    public Long create(HrPerfGoal goal) {
        if (goal.getStatus() == null) goal.setStatus("DRAFT");
        mapper.insert(goal);
        return goal.getId();
    }

    @Transactional
    public void update(HrPerfGoal goal) {
        mapper.updateById(goal);
    }

    @Transactional
    public void submit(Long id) {
        HrPerfGoal goal = mapper.selectById(id);
        goal.setStatus("SUBMITTED");
        mapper.updateById(goal);
    }

    public HrPerfGoal getById(Long id) {
        return mapper.selectById(id);
    }

    public Page<HrPerfGoal> listPage(Long cycleId, Long empId, String status, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrPerfGoal>()
                .eq(cycleId != null, HrPerfGoal::getCycleId, cycleId)
                .eq(empId != null, HrPerfGoal::getEmpId, empId)
                .eq(status != null && !status.isBlank(), HrPerfGoal::getStatus, status)
                .orderByDesc(HrPerfGoal::getCreateTime));
    }
}
