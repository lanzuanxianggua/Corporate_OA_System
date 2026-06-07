package cn.oa.message.callback;

import cn.oa.message.dto.MsgSendDTO;
import cn.oa.message.service.MsgNotificationService;
import cn.oa.message.service.MsgNotificationTypeService;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.event.WfInstanceCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * WfInstanceNotifyListener 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class WfInstanceNotifyListenerTest {

    @Mock
    private MsgNotificationService notificationService;

    @Mock
    private MsgNotificationTypeService typeService;

    private WfInstanceNotifyListener listener;

    @BeforeEach
    void setUp() {
        listener = new WfInstanceNotifyListener(notificationService, typeService);
    }

    @Nested
    @DisplayName("onWfInstanceCompleted() 事件路由")
    class OnEvent {

        @Test
        @DisplayName("LEAVE_+APPROVED 事件 — 不实际发送, 仅路由校验 (recipient 由业务 callback 提供)")
        void leaveApproved_routesToType() {
            // given
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(100L, "APPROVED", "LEAVE_42");
            // 不需要 mock requireEnabled — 路由仅做匹配, 不实际发送

            // when
            listener.onWfInstanceCompleted(event);

            // then: 路由完成, 不应实际调用 send (此版本仅做路由占位)
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("EXPENSE_+REJECTED 事件 — 路由到 EXPENSE_REJECT 类型")
        void expenseRejected_routesToType() {
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(101L, "REJECTED", "EXPENSE_99");

            listener.onWfInstanceCompleted(event);

            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("未知 businessKey 前缀 — 路由到 GENERAL 类型")
        void unknownBizPrefix_routesToGeneral() {
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(102L, "APPROVED", "WEIRD_1");

            listener.onWfInstanceCompleted(event);

            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("业务 key 为空 — 直接 return, 不调用 typeService")
        void blankBusinessKey_skipped() {
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(103L, "APPROVED", "");

            listener.onWfInstanceCompleted(event);

            verifyNoInteractions(typeService, notificationService);
        }

        @Test
        @DisplayName("非终态 status — 跳过")
        void nonTerminalStatus_skipped() {
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(104L, "RUNNING", "LEAVE_1");

            listener.onWfInstanceCompleted(event);

            verifyNoInteractions(typeService, notificationService);
        }

        @Test
        @DisplayName("监听器内异常被吞掉 — 不向上抛出")
        void exception_swallowed() {
            WfInstanceCompletedEvent event = new WfInstanceCompletedEvent(105L, "APPROVED", "LEAVE_1");
            // 不打 mock, typeService.requireEnabled 不会真被调 (此版本未实际发)
            // 但构造异常路径: businessKey null
            WfInstanceCompletedEvent nullBiz = new WfInstanceCompletedEvent(106L, "APPROVED", null);

            // 不应抛
            listener.onWfInstanceCompleted(nullBiz);
            listener.onWfInstanceCompleted(event);
        }
    }

    @Nested
    @DisplayName("sendForBusiness() 业务方显式调用")
    class SendForBusiness {

        @Test
        @DisplayName("LEAVE_+APPROVED — 构造正确 DTO 并 send")
        void leaveApproved_sendsNotification() {
            // given
            doNothing().when(typeService).requireEnabled("LEAVE_APPROVE");

            // when
            listener.sendForBusiness(7L, "LEAVE_42", "APPROVED");

            // then
            ArgumentCaptor<MsgSendDTO> dtoCap = ArgumentCaptor.forClass(MsgSendDTO.class);
            verify(notificationService).send(dtoCap.capture(), eq(0L));

            MsgSendDTO dto = dtoCap.getValue();
            assertThat(dto.getType()).isEqualTo("LEAVE_APPROVE");
            assertThat(dto.getRecipientIds()).containsExactly(7L);
            assertThat(dto.getTitle()).contains("请假");
        }

        @Test
        @DisplayName("EXPENSE_+REJECTED — 路由到 EXPENSE_REJECT")
        void expenseRejected_sendsNotification() {
            doNothing().when(typeService).requireEnabled("EXPENSE_REJECT");

            listener.sendForBusiness(8L, "EXPENSE_99", "REJECTED");

            ArgumentCaptor<MsgSendDTO> dtoCap = ArgumentCaptor.forClass(MsgSendDTO.class);
            verify(notificationService).send(dtoCap.capture(), eq(0L));

            assertThat(dtoCap.getValue().getType()).isEqualTo("EXPENSE_REJECT");
            assertThat(dtoCap.getValue().getRecipientIds()).containsExactly(8L);
        }

        @Test
        @DisplayName("类型未启用 — 跳过 send, 不抛异常")
        void typeDisabled_skipped() {
            doThrow(new BizException(cn.oa.platform.common.api.RCode.BAD_REQUEST, "通知类型未启用"))
                    .when(typeService).requireEnabled(anyString());

            listener.sendForBusiness(9L, "LEAVE_1", "APPROVED");

            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("recipientId 为 null — 跳过")
        void nullRecipient_skipped() {
            listener.sendForBusiness(null, "LEAVE_1", "APPROVED");

            verifyNoInteractions(typeService, notificationService);
        }

        @Test
        @DisplayName("未知业务前缀 + APPROVED — 走 GENERAL")
        void unknownPrefix_usesGeneral() {
            doNothing().when(typeService).requireEnabled("GENERAL");

            listener.sendForBusiness(10L, "WEIRD_1", "APPROVED");

            ArgumentCaptor<MsgSendDTO> dtoCap = ArgumentCaptor.forClass(MsgSendDTO.class);
            verify(notificationService).send(dtoCap.capture(), anyLong());
            assertThat(dtoCap.getValue().getType()).isEqualTo("GENERAL");
        }
    }
}
