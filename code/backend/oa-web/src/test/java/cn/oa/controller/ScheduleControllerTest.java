package cn.oa.controller;

import cn.oa.entity.OaSchedule;
import cn.oa.entity.dto.ScheduleDTO;
import cn.oa.service.ScheduleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
@DisplayName("日程管理 - ScheduleController")
class ScheduleControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    private ScheduleDTO buildScheduleDTO(Long id, String title) {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setId(id);
        dto.setEmpId(1L);
        dto.setTitle(title);
        dto.setContent("日程描述");
        dto.setStartTime(LocalDateTime.of(2026, 6, 1, 9, 0, 0));
        dto.setEndTime(LocalDateTime.of(2026, 6, 1, 10, 0, 0));
        dto.setStatus(0);
        return dto;
    }

    private OaSchedule buildSchedule(Long id, String title) {
        OaSchedule schedule = new OaSchedule();
        schedule.setId(id);
        schedule.setTitle(title);
        schedule.setContent("日程描述");
        schedule.setEmpId(1L);
        schedule.setStartTime(LocalDateTime.now().plusHours(1));
        schedule.setEndTime(LocalDateTime.now().plusHours(2));
        schedule.setCreateTime(LocalDateTime.now());
        return schedule;
    }

    @Test
    @DisplayName("分页查询日程")
    void pageSchedule() throws Exception {
        IPage<OaSchedule> page = new Page<>(1, 10);
        page.setTotal(2);
        page.setRecords(List.of(buildSchedule(1L, "部门会议"), buildSchedule(2L, "项目评审")));

        when(scheduleService.pageList(1, 10, 1L)).thenReturn(page);

        mockMvc.perform(get("/api/schedule/page").param("pageNum", "1").param("pageSize", "10")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].title").value("部门会议"));
    }

    @Test
    @DisplayName("分页查询日程 - 按员工")
    @SuppressWarnings("unchecked")
    void pageScheduleByEmp() throws Exception {
        IPage<OaSchedule> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildSchedule(1L, "部门会议")));

        when(scheduleService.pageList(1, 10, 1L)).thenReturn(page);

        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("roles:1")).thenReturn(List.of("ADMIN"));

        mockMvc.perform(get("/api/schedule/page")
                        .param("pageNum", "1").param("pageSize", "10").param("empId", "1")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("新增日程")
    void addSchedule() throws Exception {
        when(scheduleService.save(any(OaSchedule.class))).thenReturn(true);

        mockMvc.perform(post("/api/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildScheduleDTO(null, "新日程"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(scheduleService, times(1)).save(any(OaSchedule.class));
    }

    @Test
    @DisplayName("修改日程")
    void updateSchedule() throws Exception {
        OaSchedule existing = buildSchedule(1L, "原日程");
        when(scheduleService.getById(1L)).thenReturn(existing);
        when(scheduleService.updateById(any(OaSchedule.class))).thenReturn(true);

        mockMvc.perform(put("/api/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildScheduleDTO(1L, "修改后")))
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(scheduleService, times(1)).updateById(any(OaSchedule.class));
    }

    @Test
    @DisplayName("删除日程")
    void deleteSchedule() throws Exception {
        OaSchedule existing = buildSchedule(1L, "待删除日程");
        when(scheduleService.getById(1L)).thenReturn(existing);
        when(scheduleService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/schedule/1")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(scheduleService, times(1)).removeById(1L);
    }
}
