package cn.oa.controller;

import cn.oa.entity.OaAttendance;
import cn.oa.service.AttendanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@DisplayName("考勤管理 - AttendanceController")
class AttendanceControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @Test
    @DisplayName("上班打卡")
    void clockIn() throws Exception {
        doNothing().when(attendanceService).clockIn(anyLong());

        mockMvc.perform(post("/api/attendance/clock-in")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(attendanceService, times(1)).clockIn(1L);
    }

    @Test
    @DisplayName("下班打卡")
    void clockOut() throws Exception {
        doNothing().when(attendanceService).clockOut(anyLong());

        mockMvc.perform(post("/api/attendance/clock-out")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(attendanceService, times(1)).clockOut(1L);
    }

    @Test
    @DisplayName("获取今日考勤")
    void getToday() throws Exception {
        OaAttendance attendance = new OaAttendance();
        attendance.setId(1L);
        attendance.setEmpId(1L);
        attendance.setClockIn(LocalDateTime.of(2026, 5, 9, 8, 55, 0));
        attendance.setStatus(1);
        attendance.setWorkDate(LocalDate.now());

        when(attendanceService.getTodayAttendance(1L)).thenReturn(attendance);

        mockMvc.perform(get("/api/attendance/today")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(1));
    }
}
