package cn.oa.hr.recruitment.service;

import cn.oa.hr.recruitment.entity.HrRecruitCandidate;
import cn.oa.hr.recruitment.entity.HrRecruitOffer;
import cn.oa.hr.recruitment.mapper.HrRecruitCandidateMapper;
import cn.oa.hr.recruitment.mapper.HrRecruitOfferMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HrRecruitOfferService {
    private final HrRecruitOfferMapper mapper;
    private final HrRecruitCandidateMapper candidateMapper;

    @Transactional
    public Long create(HrRecruitOffer offer) {
        if (offer.getStatus() == null) offer.setStatus("PENDING");
        mapper.insert(offer);
        updateCandidateStatus(offer.getCandidateId(), "OFFER");
        return offer.getId();
    }

    @Transactional
    public void update(HrRecruitOffer offer) {
        mapper.updateById(offer);
    }

    @Transactional
    public void accept(Long id) {
        HrRecruitOffer offer = mapper.selectById(id);
        offer.setStatus("ACCEPTED");
        mapper.updateById(offer);
        updateCandidateStatus(offer.getCandidateId(), "HIRED");
    }

    @Transactional
    public void reject(Long id, String reason) {
        HrRecruitOffer offer = mapper.selectById(id);
        offer.setStatus("REJECTED");
        offer.setRejectReason(reason);
        mapper.updateById(offer);
        updateCandidateStatus(offer.getCandidateId(), "REJECTED");
    }

    @Transactional
    public void onboard(Long id) {
        HrRecruitOffer offer = mapper.selectById(id);
        offer.setStatus("ONBOARDED");
        mapper.updateById(offer);
        updateCandidateStatus(offer.getCandidateId(), "ONBOARDED");
    }

    public Page<HrRecruitOffer> listPage(Long candidateId, String status, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrRecruitOffer>()
                .eq(candidateId != null, HrRecruitOffer::getCandidateId, candidateId)
                .eq(status != null && !status.isBlank(), HrRecruitOffer::getStatus, status)
                .orderByDesc(HrRecruitOffer::getCreateTime));
    }

    private void updateCandidateStatus(Long candidateId, String status) {
        HrRecruitCandidate candidate = candidateMapper.selectById(candidateId);
        if (candidate != null) {
            candidate.setStatus(status);
            candidateMapper.updateById(candidate);
        }
    }
}
