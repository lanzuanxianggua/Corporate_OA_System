package cn.oa.service.impl;

import cn.oa.common.constant.BusinessType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaOuting;
import cn.oa.mapper.OaOutingMapper;
import cn.oa.service.OutingService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class OutingServiceImpl extends BaseApprovalServiceImpl<OaOutingMapper, OaOuting>
        implements OutingService {

    public OutingServiceImpl() {
        this.empIdGetter = OaOuting::getEmpId;
        this.statusGetter = OaOuting::getStatus;
        this.createTimeGetter = OaOuting::getCreateTime;
        this.idGetter = OaOuting::getId;
    }

    @Override
    protected String getBusinessType() {
        return BusinessType.OUTING;
    }

    @Override
    protected void setStatus(OaOuting entity, Integer status) {
        entity.setStatus(status);
    }

    @Override
    protected void setEmpName(OaOuting entity, String name) {
        entity.setEmpName(name);
    }

    @Override
    protected void setRemark(OaOuting entity, String remark) {
        entity.setRemark(remark);
    }

    @Override
    protected Map<String, Object> buildConditionContext(OaOuting entity) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(
                entity.getStartTime().toLocalDate(), entity.getEndTime().toLocalDate()) + 1;
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("days", days);
        return ctx;
    }

    @Override
    @Transactional
    public void submit(OaOuting outing) {
        if (outing.getStartTime() == null || outing.getEndTime() == null) {
            throw new BusinessException("外出起止时间不能为空");
        }
        doSubmit(outing);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark) {
        doApprove(applyId, approverId, status, remark);
    }

    @Override
    @Transactional
    public void approve(Long applyId, Long approverId, Integer status, String remark, Long taskId) {
        doApprove(applyId, approverId, status, remark);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        doUpdateStatus(id, status);
    }

    @Override
    public IPage<OaOuting> pageList(int pageNum, int pageSize, Long empId, Integer status) {
        return doPageList(pageNum, pageSize, empId, status);
    }
}
