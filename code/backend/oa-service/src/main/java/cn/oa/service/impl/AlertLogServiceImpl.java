package cn.oa.service.impl;

import cn.oa.entity.RptAlertLog;
import cn.oa.mapper.RptAlertLogMapper;
import cn.oa.service.AlertLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AlertLogServiceImpl extends ServiceImpl<RptAlertLogMapper, RptAlertLog> implements AlertLogService {

    @Override
    public IPage<RptAlertLog> pageList(int pageNum, int pageSize, Long ruleId, Character handleStatus) {
        Page<RptAlertLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RptAlertLog> wrapper = new LambdaQueryWrapper<>();
        if (ruleId != null) {
            wrapper.eq(RptAlertLog::getRuleId, ruleId);
        }
        if (handleStatus != null) {
            wrapper.eq(RptAlertLog::getHandleStatus, handleStatus);
        }
        wrapper.orderByDesc(RptAlertLog::getAlertTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional
    public void handle(Long logId, String handler, String handleRemark) {
        RptAlertLog log = this.getById(logId);
        if (log == null) {
            throw new RuntimeException("预警日志不存在");
        }
        log.setHandleStatus('1');
        log.setHandler(handler);
        log.setHandleRemark(handleRemark);
        log.setHandleTime(LocalDateTime.now());
        this.updateById(log);
    }
}