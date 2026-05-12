package cn.oa.controller;

import cn.oa.entity.SysDept;
import cn.oa.service.DeptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeptController.class)
@DisplayName("部门管理 - DeptController")
class DeptControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeptService deptService;

    private SysDept buildDept(Long id, String name) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setDeptName(name);
        dept.setSort(0);
        dept.setStatus(1);
        return dept;
    }

    @Test
    @DisplayName("获取部门树")
    void getDeptTree() throws Exception {
        SysDept root = buildDept(1L, "总公司");
        when(deptService.getDeptTree()).thenReturn(List.of(root));

        mockMvc.perform(get("/api/dept/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].deptName").value("总公司"));
    }

    @Test
    @DisplayName("新增部门")
    void addDept() throws Exception {
        when(deptService.save(any(SysDept.class))).thenReturn(true);
        SysDept dept = buildDept(null, "新部门");

        mockMvc.perform(post("/api/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(deptService, times(1)).save(any(SysDept.class));
    }

    @Test
    @DisplayName("修改部门")
    void updateDept() throws Exception {
        when(deptService.updateById(any(SysDept.class))).thenReturn(true);
        SysDept dept = buildDept(2L, "技术部-改名");

        mockMvc.perform(put("/api/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(deptService, times(1)).updateById(any(SysDept.class));
    }

    @Test
    @DisplayName("删除部门")
    void deleteDept() throws Exception {
        when(deptService.removeById(2L)).thenReturn(true);

        mockMvc.perform(delete("/api/dept/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(deptService, times(1)).removeById(2L);
    }
}
