package cn.oa.controller;

import cn.oa.entity.OaLoan;
import cn.oa.service.LoanService;
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

@WebMvcTest(LoanController.class)
@DisplayName("借支管理 - LoanController")
class LoanControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoanService loanService;

    private OaLoan buildLoan(Long id) {
        OaLoan loan = new OaLoan();
        loan.setId(id);
        loan.setEmpId(1L);
        loan.setLoanAmount(new BigDecimal("10000.00"));
        loan.setLoanReason("家庭急用");
        loan.setRepaymentPlan("分6期还款");
        loan.setStatus("0");
        return loan;
    }

    @Test
    @DisplayName("提交借支申请")
    void submitLoan() throws Exception {
        doNothing().when(loanService).submit(any(OaLoan.class));

        mockMvc.perform(post("/api/loan/submit")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildLoan(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(loanService, times(1)).submit(any(OaLoan.class));
    }

    @Test
    @DisplayName("审批借支申请 - 通过")
    void approveLoanPass() throws Exception {
        doNothing().when(loanService).approve(1L, 1L, 1, "同意");

        Map<String, Object> params = Map.of("id", 1, "status", 1, "remark", "同意");

        mockMvc.perform(post("/api/loan/approve")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(loanService, times(1)).approve(1L, 1L, 1, "同意");
    }

    @Test
    @DisplayName("分页查询借支申请")
    void pageLoan() throws Exception {
        IPage<OaLoan> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildLoan(1L)));

        when(loanService.pageList(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/loan/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].loanReason").value("家庭急用"));
    }

    @Test
    @DisplayName("添加还款记录")
    void repayment() throws Exception {
        doNothing().when(loanService).addRepayment(1L, new BigDecimal("2000.00"), "第1期还款");

        Map<String, Object> params = Map.of("loanId", 1, "amount", "2000.00", "remark", "第1期还款");

        mockMvc.perform(post("/api/loan/repayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(loanService, times(1)).addRepayment(1L, new BigDecimal("2000.00"), "第1期还款");
    }
}
