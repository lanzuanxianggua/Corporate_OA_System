package cn.oa.controller;

import cn.oa.entity.OaBusinessTrip;
import cn.oa.service.BusinessTripService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessTripController.class)
@DisplayName("出差管理 - BusinessTripController")
class BusinessTripControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BusinessTripService businessTripService;

    private OaBusinessTrip buildTrip(Long id) {
        OaBusinessTrip trip = new OaBusinessTrip();
        trip.setId(id);
        trip.setEmpId(1L);
        trip.setDestination("北京");
        trip.setPurpose("客户拜访");
        trip.setStartTime(LocalDateTime.of(2026, 6, 1, 9, 0, 0));
        trip.setEndTime(LocalDateTime.of(2026, 6, 3, 18, 0, 0));
        trip.setStatus(0);
        return trip;
    }

    @Test
    @DisplayName("提交出差申请")
    void submitTrip() throws Exception {
        doNothing().when(businessTripService).submit(any(OaBusinessTrip.class));

        mockMvc.perform(post("/api/business-trip/submit")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildTrip(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(businessTripService, times(1)).submit(any(OaBusinessTrip.class));
    }

    @Test
    @DisplayName("审批出差申请 - 通过")
    void approveTripPass() throws Exception {
        doNothing().when(businessTripService).approve(1L, 1L, 1, "同意", null);

        Map<String, Object> params = Map.of("id", 1, "status", 1, "remark", "同意");

        mockMvc.perform(post("/api/business-trip/approve")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(businessTripService, times(1)).approve(1L, 1L, 1, "同意", null);
    }

    @Test
    @DisplayName("审批出差申请 - 驳回")
    void approveTripReject() throws Exception {
        doNothing().when(businessTripService).approve(1L, 1L, 2, "理由不充分", null);

        Map<String, Object> params = Map.of("id", 1, "status", 2, "remark", "理由不充分");

        mockMvc.perform(post("/api/business-trip/approve")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("分页查询出差申请")
    void pageTrip() throws Exception {
        IPage<OaBusinessTrip> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildTrip(1L)));

        when(businessTripService.pageList(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/business-trip/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].destination").value("北京"));
    }
}
