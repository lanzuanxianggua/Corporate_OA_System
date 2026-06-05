package cn.oa.message.mapper;

import cn.oa.message.entity.MsgNotification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 消息通知 Mapper.
 */
@Mapper
public interface MsgNotificationMapper extends BaseMapper<MsgNotification> {

    /**
     * 按接收人 id 查询通知列表.
     */
    @Select("""
        <script>
        SELECT n.id, n.title, n.content, n.category, n.sender_id, n.priority,
               n.status, n.read_time, n.create_time, n.del_flag
        FROM msg_notification n
        INNER JOIN msg_notification_recipient nr ON n.id = nr.notification_id
        WHERE n.del_flag = '0'
          AND nr.recipient_id = #{recipientId}
        ORDER BY n.create_time DESC
        LIMIT #{limit}
        </script>
        """)
    List<MsgNotification> findByRecipientId(@Param("recipientId") Long recipientId, @Param("limit") int limit);

    /**
     * 统计接收人未读通知数.
     */
    @Select("""
        SELECT COUNT(*)
        FROM msg_notification_recipient nr
        INNER JOIN msg_notification n ON n.id = nr.notification_id
        WHERE n.del_flag = '0'
          AND nr.recipient_id = #{recipientId}
          AND nr.is_read = 'N'
        """)
    long countUnread(@Param("recipientId") Long recipientId);

    /**
     * 按接收人 id 查询通知列表 (含关联数据).
     */
    @Select("""
        <script>
        SELECT n.id, n.title, n.content, n.category, n.sender_id, n.priority,
               n.status, n.read_time, n.create_time,
               nr.id AS recipient_id, nr.is_read, nr.read_time AS recipient_read_time
        FROM msg_notification n
        INNER JOIN msg_notification_recipient nr ON n.id = nr.notification_id
        WHERE n.del_flag = '0'
          AND nr.recipient_id = #{recipientId}
        ORDER BY n.create_time DESC
        LIMIT #{limit}
        </script>
        """)
    List<Map<String, Object>> findDetailByRecipientId(@Param("recipientId") Long recipientId, @Param("limit") int limit);
}
