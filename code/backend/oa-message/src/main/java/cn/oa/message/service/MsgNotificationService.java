package cn.oa.message.service;

import cn.oa.message.entity.MsgNotification;
import cn.oa.message.vo.MsgNotificationVO;
import cn.oa.platform.core.base.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface MsgNotificationService extends IService<MsgNotification> {

    /**
     * 发送单条消息通知
     *
     * @param msgType 消息类型
     * @param empId   接收人ID
     * @param title   标题
     * @param content 内容
     * @param params  扩展参数（支持 bizId, bizType, channel 等）
     */
    void send(String msgType, Long empId, String title, String content, Map<String, Object> params);

    /**
     * 批量发送消息通知
     *
     * @param msgType 消息类型
     * @param empIds  接收人ID列表
     * @param title   标题
     * @param content 内容
     * @param params  扩展参数
     */
    void sendMulti(String msgType, List<Long> empIds, String title, String content, Map<String, Object> params);

    /**
     * 标记单条消息为已读
     */
    void markRead(Long id);

    /**
     * 标记全部已读
     */
    void markAllRead(Long empId);

    /**
     * 获取未读消息数量
     */
    long getUnreadCount(Long empId);

    /**
     * 分页查询消息通知（按 createdAt 倒序）
     */
    PageResult<MsgNotificationVO> pageQuery(Long empId, Integer pageNum, Integer pageSize);
}
