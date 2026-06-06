package cn.oa.message.service;

import cn.oa.message.dto.MsgSendDTO;
import cn.oa.message.entity.MsgNotification;
import cn.oa.message.mapper.MsgNotificationMapper;
import cn.oa.message.mapper.MsgNotificationRecipientMapper;
import cn.oa.message.vo.MsgUnreadCountVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MsgNotificationService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class MsgNotificationServiceTest {

    @Mock
    private MsgNotificationMapper mapper;

    @Mock
    private MsgNotificationRecipientMapper recipientMapper;

    @Captor
    private ArgumentCaptor<MsgNotification> notifCaptor;

    private MsgNotificationService service;

    @BeforeEach
    void setUp() {
        service = new MsgNotificationService(mapper, recipientMapper);
    }

    @Nested
    @DisplayName("send() 发送消息")
    class Send {

        @Test
        @DisplayName("发送成功 — 为每个接收人创建一条 UNREAD 消息")
        void send_success() {
            // given
            MsgSendDTO dto = new MsgSendDTO();
            dto.setTitle("审批通知");
            dto.setContent("您的请假申请已通过");
            dto.setType("APPROVAL");
            dto.setRecipientIds(List.of(10L, 20L, 30L));

            when(mapper.insert(any(MsgNotification.class))).thenReturn(1);

            // when
            service.send(dto, 1L);

            // then
            verify(mapper, times(3)).insert(notifCaptor.capture());

            List<MsgNotification> captured = notifCaptor.getAllValues();
            assertThat(captured).hasSize(3);

            MsgNotification first = captured.get(0);
            assertThat(first.getTitle()).isEqualTo("审批通知");
            assertThat(first.getContent()).isEqualTo("您的请假申请已通过");
            assertThat(first.getType()).isEqualTo("APPROVAL");
            assertThat(first.getSenderId()).isEqualTo(1L);
            assertThat(first.getRecipientId()).isEqualTo(10L);
            assertThat(first.getStatus()).isEqualTo("UNREAD");
            assertThat(first.getDelFlag()).isEqualTo("0");

            assertThat(captured.get(1).getRecipientId()).isEqualTo(20L);
            assertThat(captured.get(2).getRecipientId()).isEqualTo(30L);
        }

        @Test
        @DisplayName("单个接收人也正确发送")
        void send_singleRecipient() {
            // given
            MsgSendDTO dto = new MsgSendDTO();
            dto.setTitle("系统通知");
            dto.setContent("系统维护通知");
            dto.setType("SYSTEM");
            dto.setRecipientIds(List.of(99L));

            when(mapper.insert(any(MsgNotification.class))).thenReturn(1);

            // when
            service.send(dto, 0L);

            // then
            verify(mapper, times(1)).insert(notifCaptor.capture());
            assertThat(notifCaptor.getValue().getRecipientId()).isEqualTo(99L);
            assertThat(notifCaptor.getValue().getSenderId()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("countUnread() 未读消息数")
    class CountUnread {

        @Test
        @DisplayName("返回未读消息数")
        void countUnread_success() {
            // given
            when(mapper.countUnread(1L)).thenReturn(5L);

            // when
            MsgUnreadCountVO result = service.countUnread(1L);

            // then
            assertThat(result.getTotal()).isEqualTo(5L);
            verify(mapper).countUnread(1L);
        }

        @Test
        @DisplayName("无未读消息时返回0")
        void countUnread_zero() {
            // given
            when(mapper.countUnread(2L)).thenReturn(0L);

            // when
            MsgUnreadCountVO result = service.countUnread(2L);

            // then
            assertThat(result.getTotal()).isZero();
            verify(mapper).countUnread(2L);
        }
    }
}
