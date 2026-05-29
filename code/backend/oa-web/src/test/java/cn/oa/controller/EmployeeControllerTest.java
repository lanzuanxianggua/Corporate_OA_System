package cn.oa.controller;

import cn.oa.entity.SysEmployee;
import cn.oa.entity.dto.EmployeeDTO;
import cn.oa.service.EmployeeService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@DisplayName("员工管理 - EmployeeController")
class EmployeeControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    private SysEmployee buildEmp(Long id, String code, String name) {
        SysEmployee emp = new SysEmployee();
        emp.setId(id);
        emp.setEmpCode(code);
        emp.setEmpName(name);
        emp.setPhone("13800000000");
        emp.setEmail("test@oa.com");
        emp.setDeptId(1L);
        emp.setStatus(1);
        return emp;
    }

    private EmployeeDTO buildEmpDTO(Long id, String code, String name) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(id);
        dto.setEmpCode(code);
        dto.setEmpName(name);
        dto.setPhone("13800000000");
        dto.setEmail("test@oa.com");
        dto.setDeptId(1L);
        dto.setStatus(1);
        dto.setPostId(1L);
        return dto;
    }

    @Test
    @DisplayName("分页查询员工")
    void pageEmployee() throws Exception {
        IPage<SysEmployee> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildEmp(1L, "admin", "管理员")));

        when(employeeService.pageList(1, 10, null, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/employee/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].empCode").value("admin"));
    }

    @Test
    @DisplayName("分页查询员工 - 带筛选条件")
    void pageEmployeeWithFilter() throws Exception {
        IPage<SysEmployee> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(List.of());

        when(employeeService.pageList(1, 10, "张三", 2L, null)).thenReturn(page);

        mockMvc.perform(get("/api/employee/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("empName", "张三")
                        .param("deptId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("获取员工详情")
    void getEmployeeById() throws Exception {
        when(employeeService.getById(1L)).thenReturn(buildEmp(1L, "admin", "管理员"));

        mockMvc.perform(get("/api/employee/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.empCode").value("admin"));
    }

    @Test
    @DisplayName("新增员工")
    void addEmployee() throws Exception {
        doNothing().when(employeeService).addEmployee(any(SysEmployee.class));

        mockMvc.perform(post("/api/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEmpDTO(null, "TEST001", "新员工"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(employeeService, times(1)).addEmployee(any(SysEmployee.class));
    }

    @Test
    @DisplayName("修改员工")
    void updateEmployee() throws Exception {
        when(employeeService.updateById(any(SysEmployee.class))).thenReturn(true);

        mockMvc.perform(put("/api/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildEmpDTO(1L, "admin", "管理员-修改"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(employeeService, times(1)).updateById(any(SysEmployee.class));
    }

    @Test
    @DisplayName("删除员工")
    void deleteEmployee() throws Exception {
        when(employeeService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/employee/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(employeeService, times(1)).removeById(1L);
    }

    @Test
    @DisplayName("修改密码")
    void updatePassword() throws Exception {
        doNothing().when(employeeService).updatePassword(1L, "old123", "new456");

        mockMvc.perform(put("/api/employee/password")
                        .param("empId", "1")
                        .param("oldPwd", "old123")
                        .param("newPwd", "new456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(employeeService, times(1)).updatePassword(1L, "old123", "new456");
    }
}
