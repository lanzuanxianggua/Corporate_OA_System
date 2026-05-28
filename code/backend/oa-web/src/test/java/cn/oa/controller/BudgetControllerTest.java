package cn.oa.controller;

import cn.oa.entity.OaBudget;
import cn.oa.service.BudgetService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
@DisplayName("预算管理 - BudgetController")
class BudgetControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BudgetService budgetService;

    private OaBudget buildBudget(Long id, Long deptId, int year, int month) {
        OaBudget budget = new OaBudget();
        budget.setId(id);
        budget.setDeptId(deptId);
        budget.setBudgetYear(year);
        budget.setBudgetMonth(month);
        budget.setAmount(new BigDecimal("50000.00"));
        budget.setUsedAmount(new BigDecimal("12000.00"));
        budget.setStatus("0");
        return budget;
    }

    @Test
    @DisplayName("分页查询预算")
    void pageBudget() throws Exception {
        IPage<OaBudget> page = new Page<>(1, 10);
        page.setTotal(2);
        page.setRecords(List.of(
                buildBudget(1L, 1L, 2026, 5),
                buildBudget(2L, 2L, 2026, 5)
        ));

        when(budgetService.pageList(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/budget/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].budgetYear").value(2026));
    }

    @Test
    @DisplayName("新增预算")
    void addBudget() throws Exception {
        when(budgetService.save(any(OaBudget.class))).thenReturn(true);

        mockMvc.perform(post("/api/budget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBudget(null, 1L, 2026, 6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(budgetService, times(1)).save(any(OaBudget.class));
    }

    @Test
    @DisplayName("修改预算")
    void updateBudget() throws Exception {
        when(budgetService.updateById(any(OaBudget.class))).thenReturn(true);

        mockMvc.perform(put("/api/budget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBudget(1L, 1L, 2026, 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(budgetService, times(1)).updateById(any(OaBudget.class));
    }

    @Test
    @DisplayName("删除预算")
    void deleteBudget() throws Exception {
        when(budgetService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/budget/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(budgetService, times(1)).removeById(1L);
    }

    @Test
    @DisplayName("查询部门月度预算")
    void getByDeptMonth() throws Exception {
        OaBudget budget = buildBudget(1L, 1L, 2026, 5);

        when(budgetService.getByDeptMonth(1L, 2026, 5)).thenReturn(budget);

        mockMvc.perform(get("/api/budget/dept/1/month")
                        .param("year", "2026")
                        .param("month", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.amount").value(50000.00));
    }
}
