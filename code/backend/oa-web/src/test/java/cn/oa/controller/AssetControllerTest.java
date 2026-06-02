package cn.oa.controller;

import cn.oa.entity.OaAsset;
import cn.oa.entity.OaAssetBorrow;
import cn.oa.service.AssetBorrowService;
import cn.oa.service.AssetService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@DisplayName("资产管理 - AssetController")
class AssetControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssetService assetService;

    @MockitoBean
    private AssetBorrowService assetBorrowService;

    private OaAsset buildAsset(Long id, String code, String name) {
        OaAsset asset = new OaAsset();
        asset.setId(id);
        asset.setAssetCode(code);
        asset.setAssetName(name);
        asset.setCategory("electronics");
        asset.setPurchaseDate(LocalDate.of(2026, 1, 15));
        asset.setPurchasePrice(new BigDecimal("5999.00"));
        asset.setStatus("0");
        return asset;
    }

    private OaAssetBorrow buildBorrow(Long id, Long assetId) {
        OaAssetBorrow borrow = new OaAssetBorrow();
        borrow.setId(id);
        borrow.setAssetId(assetId);
        borrow.setBorrowerId(1L);
        borrow.setBorrowTime(LocalDateTime.of(2026, 5, 20, 9, 0, 0));
        borrow.setExpectedReturn(LocalDateTime.of(2026, 5, 27, 18, 0, 0));
        borrow.setStatus("0");
        return borrow;
    }

    @Test
    @DisplayName("分页查询资产")
    void pageAsset() throws Exception {
        IPage<OaAsset> page = new Page<>(1, 10);
        page.setTotal(2);
        page.setRecords(List.of(
                buildAsset(1L, "A001", "笔记本电脑"),
                buildAsset(2L, "A002", "投影仪")
        ));

        when(assetService.pageList(1, 10, null, null, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/asset/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].assetCode").value("A001"));
    }

    @Test
    @DisplayName("新增资产")
    void addAsset() throws Exception {
        when(assetService.save(any(OaAsset.class))).thenReturn(true);

        mockMvc.perform(post("/api/asset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAsset(null, "A003", "显示器"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(assetService, times(1)).save(any(OaAsset.class));
    }

    @Test
    @DisplayName("修改资产")
    void updateAsset() throws Exception {
        when(assetService.updateById(any(OaAsset.class))).thenReturn(true);

        mockMvc.perform(put("/api/asset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAsset(1L, "A001", "笔记本电脑-修改"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(assetService, times(1)).updateById(any(OaAsset.class));
    }

    @Test
    @DisplayName("删除资产")
    void deleteAsset() throws Exception {
        when(assetService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/asset/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(assetService, times(1)).removeById(1L);
    }

    @Test
    @DisplayName("借出资产")
    void borrowAsset() throws Exception {
        doNothing().when(assetBorrowService).borrowAsset(any(OaAssetBorrow.class));

        OaAssetBorrow borrow = buildBorrow(null, 1L);

        mockMvc.perform(post("/api/asset/borrow")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(assetBorrowService, times(1)).borrowAsset(any(OaAssetBorrow.class));
    }

    @Test
    @DisplayName("归还资产")
    void returnAsset() throws Exception {
        OaAssetBorrow borrow = buildBorrow(1L, 1L);
        when(assetBorrowService.getById(1L)).thenReturn(borrow);
        doNothing().when(assetBorrowService).returnAsset(anyLong());

        mockMvc.perform(post("/api/asset/return/1")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(assetBorrowService, times(1)).returnAsset(1L);
    }

    @Test
    @DisplayName("分页查询借用记录")
    void borrowPage() throws Exception {
        IPage<OaAssetBorrow> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(buildBorrow(1L, 1L)));

        when(assetBorrowService.pageList(1, 10, null, null)).thenReturn(page);

        mockMvc.perform(get("/api/asset/borrow/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}
