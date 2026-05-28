package cn.oa.controller;

import cn.oa.entity.OaExpense;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.ExpenseService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
@DisplayName("经费管理 - ExpenseController")
class ExpenseControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private SysEmployeeMapper employeeMapper;

    private OaExpense buildExpense(Long id) {
        OaExpense expense = new OaExpense();
        expense.setId(id);
        expense.setEmpId(1L);
        expense.setTitle("团建活动费用");
        expense.setAmount(new BigDecimal("5000.00"));
        expense.setCategory("activity");
        expense.setDescription("部门团建");
        expense.setStatus(0);
        return expense;
    }

    @Test
    @DisplayName("提交经费申请")
    void submitExpense() throws Exception {
        doNothing().when(expenseService).submit(any(OaExpense.class));

        mockMvc.perform(post("/api/expense/submit")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildExpense(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(expenseService, times(1)).submit(any(OaExpense.class));
    }

    @Test
    @DisplayName("审批经费申请 - 通过")
    void approveExpensePass() throws Exception {
        doNothing().when(expenseService).approve(1L, 1L, 1, "同意");

        Map<String, Object> params = Map.of("id", 1, "status", 1, "remark", "同意");

        mockMvc.perform(post("/api/expense/approve")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(expenseService, times(1)).approve(1L, 1L, 1, "同意");
    }

    @Test
    @DisplayName("审批经费申请 - 驳回")
    void approveExpenseReject() throws Exception {
        doNothing().when(expenseService).approve(1L, 1L, 2, "超出预算");

        Map<String, Object> params = Map.of("id", 1, "status", 2, "remark", "超出预算");

        mockMvc.perform(post("/api/expense/approve")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("分页查询经费申请")
    void pageExpense() throws Exception {
        IPage<OaExpense> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildExpense(1L)));

        when(expenseService.pageList(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/expense/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("团建活动费用"));
    }
}
