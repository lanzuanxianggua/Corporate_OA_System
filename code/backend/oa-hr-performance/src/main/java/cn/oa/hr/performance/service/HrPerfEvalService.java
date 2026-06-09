package cn.oa.hr.performance.service;

import cn.oa.hr.performance.entity.HrPerfEval;
import cn.oa.hr.performance.entity.HrPerfGoal;
import cn.oa.hr.performance.mapper.HrPerfEvalMapper;
import cn.oa.hr.performance.mapper.HrPerfGoalMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrPerfEvalService {
    private final HrPerfEvalMapper mapper;
    private final HrPerfGoalMapper goalMapper;

    @Transactional
    public Long create(HrPerfEval eval) {
        if (eval.getEvalType() == null) eval.setEvalType("SELF");
        if (eval.getStatus() == null) eval.setStatus("DRAFT");
        mapper.insert(eval);
        return eval.getId();
    }

    @Transactional
    public void update(HrPerfEval eval) {
        mapper.updateById(eval);
    }

    @Transactional
    public void submit(Long id) {
        HrPerfEval eval = mapper.selectById(id);
        eval.setStatus("SUBMITTED");
        mapper.updateById(eval);

        HrPerfGoal goal = goalMapper.selectById(eval.getGoalId());
        if (goal != null && eval.getScore() != null) {
            goal.setScore(eval.getScore());
            goal.setGrade(toGrade(eval.getScore().doubleValue()));
            goal.setStatus("EVALUATED");
            goalMapper.updateById(goal);
        }
    }

    public Page<HrPerfEval> listPage(Long goalId, Long evaluatorId, String status, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrPerfEval>()
                .eq(goalId != null, HrPerfEval::getGoalId, goalId)
                .eq(evaluatorId != null, HrPerfEval::getEvaluatorId, evaluatorId)
                .eq(status != null && !status.isBlank(), HrPerfEval::getStatus, status)
                .orderByDesc(HrPerfEval::getCreateTime));
    }

    private static String toGrade(double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "E";
    }
}
