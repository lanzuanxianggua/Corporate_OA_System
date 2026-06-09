package cn.oa.hr.recruitment.service;

import cn.oa.hr.recruitment.entity.HrRecruitCandidate;
import cn.oa.hr.recruitment.entity.HrRecruitInterview;
import cn.oa.hr.recruitment.mapper.HrRecruitCandidateMapper;
import cn.oa.hr.recruitment.mapper.HrRecruitInterviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrRecruitInterviewService {
    private final HrRecruitInterviewMapper mapper;
    private final HrRecruitCandidateMapper candidateMapper;

    @Transactional
    public Long create(HrRecruitInterview interview) {
        mapper.insert(interview);
        HrRecruitCandidate candidate = candidateMapper.selectById(interview.getCandidateId());
        if (candidate != null) {
            candidate.setStatus("INTERVIEW");
            candidate.setInterviewerId(interview.getInterviewerId());
            candidateMapper.updateById(candidate);
        }
        return interview.getId();
    }

    @Transactional
    public void update(HrRecruitInterview interview) {
        mapper.updateById(interview);
        if (interview.getCandidateId() != null && interview.getScore() != null) {
            HrRecruitCandidate candidate = candidateMapper.selectById(interview.getCandidateId());
            if (candidate != null) {
                candidate.setInterviewScore(interview.getScore());
                candidate.setStatus("PASS".equals(interview.getResult()) ? "PASSED" : candidate.getStatus());
                candidateMapper.updateById(candidate);
            }
        }
    }

    public Page<HrRecruitInterview> listPage(Long candidateId, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrRecruitInterview>()
                .eq(candidateId != null, HrRecruitInterview::getCandidateId, candidateId)
                .orderByDesc(HrRecruitInterview::getInterviewDate));
    }
}
