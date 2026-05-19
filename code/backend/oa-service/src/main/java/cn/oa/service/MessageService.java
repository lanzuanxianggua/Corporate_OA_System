package cn.oa.service;

import cn.oa.entity.OaMessage;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MessageService extends IService<OaMessage> {

    void send(OaMessage message);

    void markAsRead(Long msgId, Long empId);

    Long getUnreadCount(Long empId);

    IPage<OaMessage> pageList(int pageNum, int pageSize, Long receiverId);
}
