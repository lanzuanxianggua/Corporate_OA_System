package cn.oa.controller;

import cn.oa.entity.OaAttendanceGroup;
import cn.oa.service.AttendanceGroupService;
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

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceGroupController.class)
@DisplayName("考勤组管理 - AttendanceGroupController")
class AttendanceGroupControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AttendanceGroupService attendanceGroupService;

    private OaAttendanceGroup buildGroup(Long id, String name) {
        OaAttendanceGroup group = new OaAttendanceGroup();
        group.setId(id);
        group.setGroupName(name);
        group.setWorkStart(LocalTime.of(9, 0));
        group.setWorkEnd(LocalTime.of(18, 0));
        group.setLateThreshold(15);
        group.setStatus('0');
        return group;
    }

    @Test
    @DisplayName("分页查询考勤组")
    void pageGroup() throws Exception {
        IPage<OaAttendanceGroup> page = new Page<>(1, 10);
        page.setTotal(2);
        page.setRecords(List.of(buildGroup(1L, "研发组"), buildGroup(2L, "产品组")));

        when(attendanceGroupService.pageList(1, 10, null)).thenReturn(page);

        mockMvc.perform(get("/api/attendance-group/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].groupName").value("研发组"));
    }

    @Test
    @DisplayName("新增考勤组")
    void addGroup() throws Exception {
        when(attendanceGroupService.save(any(OaAttendanceGroup.class))).thenReturn(true);

        mockMvc.perform(post("/api/attendance-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildGroup(null, "新考勤组"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(attendanceGroupService, times(1)).save(any(OaAttendanceGroup.class));
    }

    @Test
    @DisplayName("修改考勤组")
    void updateGroup() throws Exception {
        when(attendanceGroupService.updateById(any(OaAttendanceGroup.class))).thenReturn(true);

        mockMvc.perform(put("/api/attendance-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildGroup(1L, "修改后考勤组"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(attendanceGroupService, times(1)).updateById(any(OaAttendanceGroup.class));
    }

    @Test
    @DisplayName("删除考勤组")
    void deleteGroup() throws Exception {
        when(attendanceGroupService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/attendance-group/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(attendanceGroupService, times(1)).removeById(1L);
    }

    @Test
    @DisplayName("分配员工到考勤组")
    void assignEmployees() throws Exception {
        doNothing().when(attendanceGroupService).assignEmployees(1L, List.of(10L, 20L, 30L));

        Map<String, List<Long>> params = Map.of("empIds", List.of(10L, 20L, 30L));

        mockMvc.perform(post("/api/attendance-group/1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(attendanceGroupService, times(1)).assignEmployees(1L, List.of(10L, 20L, 30L));
    }

    @Test
    @DisplayName("从考勤组移除员工")
    void removeEmployees() throws Exception {
        doNothing().when(attendanceGroupService).removeEmployees(1L, List.of(10L));

        Map<String, List<Long>> params = Map.of("empIds", List.of(10L));

        mockMvc.perform(post("/api/attendance-group/1/employees/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(attendanceGroupService, times(1)).removeEmployees(1L, List.of(10L));
    }
}
