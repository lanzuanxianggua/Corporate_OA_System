package cn.oa.service;

import cn.oa.entity.OaMessage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MessageService extends IService<OaMessage> {

    /**
     * 发送消息
     */
    void send(OaMessage message);

    /**
     * 标记消息为已读
     */
    void markAsRead(Long msgId);

    /**
     * 获取未读消息数量
     */
    Long getUnreadCount(Long empId);
}
