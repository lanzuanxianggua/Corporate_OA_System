package cn.oa.service.impl;

import cn.oa.entity.OaSchedule;
import cn.oa.mapper.OaScheduleMapper;
import cn.oa.service.ScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScheduleServiceImpl extends ServiceImpl<OaScheduleMapper, OaSchedule> implements ScheduleService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public IPage<OaSchedule> pageList(int pageNum, int pageSize, Long empId) {
        Page<OaSchedule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaSchedule::getEmpId, empId);
        wrapper.orderByDesc(OaSchedule::getStartTime);
        return this.page(page, wrapper);
    }
}
