package cn.oa.hr.recruitment.service;

import cn.oa.hr.recruitment.entity.HrRecruitCandidate;
import cn.oa.hr.recruitment.mapper.HrRecruitCandidateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrRecruitCandidateService {
    private final HrRecruitCandidateMapper mapper;

    @Transactional
    public Long create(HrRecruitCandidate candidate) {
        if (candidate.getStatus() == null) candidate.setStatus("NEW");
        mapper.insert(candidate);
        return candidate.getId();
    }

    @Transactional
    public void update(HrRecruitCandidate candidate) {
        mapper.updateById(candidate);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        HrRecruitCandidate candidate = mapper.selectById(id);
        candidate.setStatus(status);
        mapper.updateById(candidate);
    }

    public HrRecruitCandidate getById(Long id) {
        return mapper.selectById(id);
    }

    public Page<HrRecruitCandidate> listPage(Long jobId, String status, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrRecruitCandidate>()
                .eq(jobId != null, HrRecruitCandidate::getJobId, jobId)
                .eq(status != null && !status.isBlank(), HrRecruitCandidate::getStatus, status)
                .orderByDesc(HrRecruitCandidate::getCreateTime));
    }
}
