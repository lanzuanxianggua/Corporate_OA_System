package cn.oa.hr.training.service;

import cn.oa.hr.training.entity.HrTrainPlan;
import cn.oa.hr.training.mapper.HrTrainPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrTrainPlanService {
    private final HrTrainPlanMapper mapper;

    @Transactional
    public Long create(HrTrainPlan plan) {
        if (plan.getStatus() == null) plan.setStatus("DRAFT");
        mapper.insert(plan);
        return plan.getId();
    }

    @Transactional
    public void update(HrTrainPlan plan) {
        mapper.updateById(plan);
    }

    @Transactional
    public void publish(Long id) {
        HrTrainPlan plan = mapper.selectById(id);
        plan.setStatus("PUBLISHED");
        mapper.updateById(plan);
    }

    public Page<HrTrainPlan> listPage(Integer year, String status, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrTrainPlan>()
                .eq(year != null, HrTrainPlan::getYear, year)
                .eq(status != null && !status.isBlank(), HrTrainPlan::getStatus, status)
                .orderByDesc(HrTrainPlan::getCreateTime));
    }
}
