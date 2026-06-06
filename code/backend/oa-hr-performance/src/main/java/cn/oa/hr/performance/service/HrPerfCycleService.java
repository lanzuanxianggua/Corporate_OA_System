package cn.oa.hr.performance.service;
import cn.oa.hr.performance.entity.HrPerfCycle;
import cn.oa.hr.performance.mapper.HrPerfCycleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@RequiredArgsConstructor @Service
public class HrPerfCycleService {
    private final HrPerfCycleMapper mapper;
    @Transactional public Long create(HrPerfCycle c) { mapper.insert(c); return c.getId(); }
    @Transactional public void update(HrPerfCycle c) { mapper.updateById(c); }
    public HrPerfCycle getById(Long id) { return mapper.selectById(id); }
    public Page<HrPerfCycle> listPage(int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrPerfCycle>().orderByDesc(HrPerfCycle::getCreateTime));
    }
}