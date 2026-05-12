package cn.oa.service;

import cn.oa.entity.OaSchedule;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ScheduleService extends IService<OaSchedule> {

    /**
     * 分页查询日程列表
     */
    IPage<OaSchedule> pageList(int pageNum, int pageSize, Long empId);
}
