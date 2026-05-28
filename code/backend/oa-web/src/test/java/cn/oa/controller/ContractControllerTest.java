package cn.oa.controller;

import cn.oa.entity.OaContract;
import cn.oa.service.ContractService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractController.class)
@DisplayName("合同管理 - ContractController")
class ContractControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContractService contractService;

    private OaContract buildContract(Long id, String name) {
        OaContract contract = new OaContract();
        contract.setId(id);
        contract.setContractNo("HT-" + (id != null ? id : "NEW"));
        contract.setContractName(name);
        contract.setContractType("purchase");
        contract.setPartyA("甲方公司");
        contract.setPartyB("乙方公司");
        contract.setAmount(new BigDecimal("100000.00"));
        contract.setSignDate(LocalDate.of(2026, 1, 10));
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2026, 12, 31));
        contract.setStatus("0");
        return contract;
    }

    @Test
    @DisplayName("分页查询合同")
    void pageContract() throws Exception {
        IPage<OaContract> page = new Page<>(1, 10);
        page.setTotal(2);
        page.setRecords(List.of(
                buildContract(1L, "采购合同A"),
                buildContract(2L, "服务合同B")
        ));

        when(contractService.pageList(1, 10, null, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/contract/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].contractName").value("采购合同A"));
    }

    @Test
    @DisplayName("新增合同")
    void addContract() throws Exception {
        when(contractService.save(any(OaContract.class))).thenReturn(true);

        mockMvc.perform(post("/api/contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildContract(null, "新合同"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(contractService, times(1)).save(any(OaContract.class));
    }

    @Test
    @DisplayName("修改合同")
    void updateContract() throws Exception {
        when(contractService.updateById(any(OaContract.class))).thenReturn(true);

        mockMvc.perform(put("/api/contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildContract(1L, "修改后合同"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(contractService, times(1)).updateById(any(OaContract.class));
    }

    @Test
    @DisplayName("删除合同")
    void deleteContract() throws Exception {
        when(contractService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/contract/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(contractService, times(1)).removeById(1L);
    }

    @Test
    @DisplayName("查询即将到期合同")
    void expiringContracts() throws Exception {
        when(contractService.expiringList(30)).thenReturn(List.of(buildContract(3L, "即将到期合同")));

        mockMvc.perform(get("/api/contract/expiring")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].contractName").value("即将到期合同"));
    }
}
