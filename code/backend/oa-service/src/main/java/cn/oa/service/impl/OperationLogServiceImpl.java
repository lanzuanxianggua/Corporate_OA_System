package cn.oa.service.impl;

import cn.oa.common.result.PageResult;
import cn.oa.entity.OaOperationLog;
import cn.oa.mapper.OaOperationLogMapper;
import cn.oa.service.OperationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OaOperationLogMapper, OaOperationLog> implements OperationLogService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<OaOperationLog> pageList(int pageNum, int pageSize, String module, String startTime, String endTime) {
        LambdaQueryWrapper<OaOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isEmpty()) {
            wrapper.like(OaOperationLog::getModule, module);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OaOperationLog::getCreateTime, LocalDateTime.parse(startTime, FMT));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OaOperationLog::getCreateTime, LocalDateTime.parse(endTime, FMT));
        }
        wrapper.orderByDesc(OaOperationLog::getCreateTime);

        Page<OaOperationLog> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }
}
