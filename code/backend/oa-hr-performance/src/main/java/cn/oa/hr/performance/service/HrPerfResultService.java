package cn.oa.hr.performance.service;
import cn.oa.hr.performance.entity.HrPerfResult;
import cn.oa.hr.performance.mapper.HrPerfResultMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@RequiredArgsConstructor @Service
public class HrPerfResultService {
    private final HrPerfResultMapper mapper;
    public Page<HrPerfResult> listPage(Long cycleId, Long empId, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps),
                new LambdaQueryWrapper<HrPerfResult>()
                .eq(cycleId!=null, HrPerfResult::getCycleId, cycleId)
                .eq(empId!=null, HrPerfResult::getEmpId, empId));
    }
}