package cn.oa.controller;

import cn.oa.entity.OaMeeting;
import cn.oa.entity.OaMeetingRoom;
import cn.oa.entity.dto.MeetingDTO;
import cn.oa.service.MeetingRoomService;
import cn.oa.service.MeetingService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
@DisplayName("会议管理 - MeetingController")
class MeetingControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MeetingRoomService meetingRoomService;

    @MockitoBean
    private MeetingService meetingService;

    private OaMeetingRoom buildRoom(Long id, String name) {
        OaMeetingRoom room = new OaMeetingRoom();
        room.setId(id);
        room.setRoomName(name);
        room.setLocation("3楼");
        room.setCapacity(20);
        room.setEquipment("投影仪,白板");
        room.setStatus("0");
        return room;
    }

    @Test
    @DisplayName("会议室列表")
    void roomList() throws Exception {
        when(meetingRoomService.list()).thenReturn(List.of(buildRoom(1L, "第一会议室"), buildRoom(2L, "第二会议室")));

        mockMvc.perform(get("/api/meeting/room/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].roomName").value("第一会议室"));
    }

    @Test
    @DisplayName("新增会议室")
    void addRoom() throws Exception {
        when(meetingRoomService.save(any(OaMeetingRoom.class))).thenReturn(true);

        mockMvc.perform(post("/api/meeting/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRoom(null, "新会议室"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(meetingRoomService, times(1)).save(any(OaMeetingRoom.class));
    }

    @Test
    @DisplayName("修改会议室")
    void updateRoom() throws Exception {
        when(meetingRoomService.updateById(any(OaMeetingRoom.class))).thenReturn(true);

        mockMvc.perform(put("/api/meeting/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRoom(1L, "修改后会议室"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(meetingRoomService, times(1)).updateById(any(OaMeetingRoom.class));
    }

    @Test
    @DisplayName("删除会议室")
    void deleteRoom() throws Exception {
        when(meetingRoomService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/meeting/room/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(meetingRoomService, times(1)).removeById(1L);
    }

    @Test
    @DisplayName("创建会议")
    void submitMeeting() throws Exception {
        doNothing().when(meetingService).submit(any(OaMeeting.class));

        MeetingDTO dto = new MeetingDTO();
        dto.setTitle("项目评审会");
        dto.setRoomId(1L);
        dto.setStartTime(LocalDateTime.of(2026, 6, 1, 14, 0, 0));
        dto.setEndTime(LocalDateTime.of(2026, 6, 1, 16, 0, 0));
        dto.setDescription("Q2项目评审");
        dto.setParticipants("1,2,3");
        dto.setStatus("0");

        mockMvc.perform(post("/api/meeting/submit")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(meetingService, times(1)).submit(any(OaMeeting.class));
    }

    @Test
    @DisplayName("我的会议列表")
    void pageMeeting() throws Exception {
        OaMeeting meeting = new OaMeeting();
        meeting.setId(1L);
        meeting.setTitle("项目评审会");
        meeting.setRoomId(1L);
        meeting.setOrganizerId(1L);

        IPage<OaMeeting> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(meeting));

        when(meetingService.pageList(1, 10, null)).thenReturn(page);

        mockMvc.perform(get("/api/meeting/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("项目评审会"));
    }

    @Test
    @DisplayName("取消会议")
    void cancelMeeting() throws Exception {
        doNothing().when(meetingService).cancel(anyLong(), anyLong());

        mockMvc.perform(post("/api/meeting/cancel/1")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(meetingService, times(1)).cancel(1L, 1L);
    }
}
