package cn.oa.controller;

import cn.oa.entity.OaPurchase;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.PurchaseService;
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

@WebMvcTest(PurchaseController.class)
@DisplayName("采购管理 - PurchaseController")
class PurchaseControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PurchaseService purchaseService;

    @MockitoBean
    private SysEmployeeMapper employeeMapper;

    private OaPurchase buildPurchase(Long id) {
        OaPurchase purchase = new OaPurchase();
        purchase.setId(id);
        purchase.setEmpId(1L);
        purchase.setItemName("办公电脑");
        purchase.setQuantity(5);
        purchase.setAmount(new BigDecimal("29999.00"));
        purchase.setReason("部门办公设备更新");
        purchase.setStatus(0);
        return purchase;
    }

    @Test
    @DisplayName("提交采购申请")
    void submitPurchase() throws Exception {
        doNothing().when(purchaseService).submit(any(OaPurchase.class));

        mockMvc.perform(post("/api/purchase/submit")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildPurchase(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(purchaseService, times(1)).submit(any(OaPurchase.class));
    }

    @Test
    @DisplayName("审批采购申请 - 通过")
    void approvePurchasePass() throws Exception {
        doNothing().when(purchaseService).approve(1L, 1L, 1, "同意", null);

        Map<String, Object> params = Map.of("id", 1, "status", 1, "remark", "同意");

        mockMvc.perform(post("/api/purchase/approve")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(purchaseService, times(1)).approve(1L, 1L, 1, "同意", null);
    }

    @Test
    @DisplayName("审批采购申请 - 驳回")
    void approvePurchaseReject() throws Exception {
        doNothing().when(purchaseService).approve(1L, 1L, 2, "预算不足", null);

        Map<String, Object> params = Map.of("id", 1, "status", 2, "remark", "预算不足");

        mockMvc.perform(post("/api/purchase/approve")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("分页查询采购申请")
    void pagePurchase() throws Exception {
        IPage<OaPurchase> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildPurchase(1L)));

        when(purchaseService.pageList(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/purchase/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].itemName").value("办公电脑"));
    }
}
