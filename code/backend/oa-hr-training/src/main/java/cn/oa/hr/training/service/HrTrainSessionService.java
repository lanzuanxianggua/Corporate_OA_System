package cn.oa.hr.training.service;

import cn.oa.hr.training.entity.HrTrainSession;
import cn.oa.hr.training.mapper.HrTrainSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HrTrainSessionService {
    private final HrTrainSessionMapper mapper;

    @Transactional
    public Long create(HrTrainSession session) {
        if (session.getStatus() == null) session.setStatus("PENDING");
        if (session.getEnrolledNum() == null) session.setEnrolledNum(0);
        if (session.getMaxCapacity() == null) session.setMaxCapacity(30);
        if (session.getSignCode() == null) session.setSignCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        mapper.insert(session);
        return session.getId();
    }

    @Transactional
    public void update(HrTrainSession session) {
        mapper.updateById(session);
    }

    @Transactional
    public void start(Long id) {
        HrTrainSession session = mapper.selectById(id);
        session.setStatus("OPEN");
        mapper.updateById(session);
    }

    @Transactional
    public void close(Long id) {
        HrTrainSession session = mapper.selectById(id);
        session.setStatus("CLOSED");
        mapper.updateById(session);
    }

    public Page<HrTrainSession> listPage(Long planId, String status, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrTrainSession>()
                .eq(planId != null, HrTrainSession::getPlanId, planId)
                .eq(status != null && !status.isBlank(), HrTrainSession::getStatus, status)
                .orderByDesc(HrTrainSession::getStartTime));
    }
}
