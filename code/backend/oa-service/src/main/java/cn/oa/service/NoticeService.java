package cn.oa.service;

import cn.oa.entity.OaNotice;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface NoticeService extends IService<OaNotice> {

    /**
     * 分页查询公告列表
     */
    IPage<OaNotice> pageList(int pageNum, int pageSize);

    /**
     * 标记公告为已读
     */
    void markAsRead(Long noticeId, Long empId);

    /**
     * 判断是否已读
     */
    boolean isRead(Long noticeId, Long empId);
}
