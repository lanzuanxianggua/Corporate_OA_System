package cn.oa.controller;

import cn.oa.entity.OaEmpArchive;
import cn.oa.service.EmpArchiveService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmpArchiveController.class)
@DisplayName("员工档案管理 - EmpArchiveController")
class EmpArchiveControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmpArchiveService empArchiveService;

    private OaEmpArchive buildArchive(Long id, Long empId) {
        OaEmpArchive archive = new OaEmpArchive();
        archive.setId(id);
        archive.setEmpId(empId);
        archive.setEducation("本科");
        archive.setMajor("计算机科学");
        archive.setGraduateSchool("北京大学");
        archive.setEntryDate(LocalDate.of(2024, 7, 1));
        archive.setProbationEndDate(LocalDate.of(2024, 10, 1));
        return archive;
    }

    @Test
    @DisplayName("根据员工ID查询档案")
    void getByEmpId() throws Exception {
        when(empArchiveService.getByEmpIdWithInfo(1L)).thenReturn(buildArchive(1L, 1L));

        mockMvc.perform(get("/api/emp-archive/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.education").value("本科"));
    }

    @Test
    @DisplayName("创建员工档案")
    void createArchive() throws Exception {
        when(empArchiveService.getByEmpId(1L)).thenReturn(null);
        when(empArchiveService.save(any(OaEmpArchive.class))).thenReturn(true);

        mockMvc.perform(post("/api/emp-archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildArchive(null, 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(empArchiveService, times(1)).save(any(OaEmpArchive.class));
    }

    @Test
    @DisplayName("更新员工档案")
    void updateArchive() throws Exception {
        when(empArchiveService.updateById(any(OaEmpArchive.class))).thenReturn(true);

        mockMvc.perform(post("/api/emp-archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildArchive(1L, 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(empArchiveService, times(1)).updateById(any(OaEmpArchive.class));
    }

    @Test
    @DisplayName("分页查询员工档案")
    void pageArchive() throws Exception {
        IPage<OaEmpArchive> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildArchive(1L, 1L)));

        when(empArchiveService.pageWithEmpInfo(1, 10, null)).thenReturn(page);

        mockMvc.perform(get("/api/emp-archive/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}
