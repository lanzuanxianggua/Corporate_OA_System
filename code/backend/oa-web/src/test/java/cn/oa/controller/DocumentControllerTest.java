package cn.oa.controller;

import cn.oa.entity.OaDocument;
import cn.oa.service.DocumentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@DisplayName("文档管理 - DocumentController")
class DocumentControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    private OaDocument buildDoc(Long id, String name) {
        OaDocument doc = new OaDocument();
        doc.setId(id);
        doc.setDocName(name);
        doc.setFilePath("/upload/" + name);
        doc.setFileSize(1024L);
        doc.setUploaderId(1L);
        doc.setCreateTime(LocalDateTime.now());
        return doc;
    }

    @Test
    @DisplayName("分页查询文档")
    void pageDocument() throws Exception {
        IPage<OaDocument> page = new Page<>(1, 10);
        page.setTotal(2);
        page.setRecords(List.of(buildDoc(1L, "测试文档.pdf"), buildDoc(2L, "需求文档.docx")));

        when(documentService.pageList(1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/document/page").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].docName").value("测试文档.pdf"));
    }

    @Test
    @DisplayName("上传文档")
    void uploadDocument() throws Exception {
        doNothing().when(documentService).upload(any(), anyLong());

        MockMultipartFile file = new MockMultipartFile(
                "file", "测试文件.pdf", "application/pdf", "test content".getBytes());

        mockMvc.perform(multipart("/api/document/upload")
                        .file(file)
                        .param("uploaderId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(documentService, times(1)).upload(any(), eq(1L));
    }

    @Test
    @DisplayName("删除文档")
    void deleteDocument() throws Exception {
        when(documentService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/document/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(documentService, times(1)).removeById(1L);
    }
}
