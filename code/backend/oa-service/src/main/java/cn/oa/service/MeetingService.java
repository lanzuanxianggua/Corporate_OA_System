package cn.oa.service;

import cn.oa.entity.OaMeeting;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MeetingService extends IService<OaMeeting> {

    void submit(OaMeeting meeting);

    void cancel(Long meetingId, Long organizerId);

    IPage<OaMeeting> pageList(int pageNum, int pageSize, Long organizerId);
}
