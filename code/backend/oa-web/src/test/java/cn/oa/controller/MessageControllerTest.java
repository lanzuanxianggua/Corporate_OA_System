package cn.oa.controller;

import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@DisplayName("消息管理 - MessageController")
class MessageControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private SysEmployeeMapper employeeMapper;

    @Test
    @DisplayName("获取未读消息数量")
    void unreadCount() throws Exception {
        when(messageService.getUnreadCount(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/message/unread-count").requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("获取未读消息数量 - 为0")
    void unreadCountZero() throws Exception {
        when(messageService.getUnreadCount(1L)).thenReturn(0L);

        mockMvc.perform(get("/api/message/unread-count").requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    @DisplayName("发送消息")
    void sendMessage() throws Exception {
        doNothing().when(messageService).send(any());

        mockMvc.perform(post("/api/message/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\":2,\"content\":\"你好，这是测试消息\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(messageService, times(1)).send(any());
    }

    @Test
    @DisplayName("标记消息已读")
    void markAsRead() throws Exception {
        doNothing().when(messageService).markAsRead(1L);

        mockMvc.perform(post("/api/message/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(messageService, times(1)).markAsRead(1L);
    }
}
