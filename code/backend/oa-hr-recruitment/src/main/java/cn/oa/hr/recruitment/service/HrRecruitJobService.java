package cn.oa.hr.recruitment.service;
import cn.oa.hr.recruitment.entity.HrRecruitJob;
import cn.oa.hr.recruitment.mapper.HrRecruitJobMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@RequiredArgsConstructor @Service
public class HrRecruitJobService {
    private final HrRecruitJobMapper mapper;
    @Transactional public Long create(HrRecruitJob j) { mapper.insert(j); return j.getId(); }
    @Transactional public void update(HrRecruitJob j) { mapper.updateById(j); }
    public HrRecruitJob getById(Long id) { return mapper.selectById(id); }
    public Page<HrRecruitJob> listPage(String status, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrRecruitJob>().eq(status!=null, HrRecruitJob::getStatus, status).orderByDesc(HrRecruitJob::getCreateTime));
    }
}