package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaDocument;
import cn.oa.service.DocumentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/document")
@Tag(name = "文档管理")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Value("${oa.upload.path:uploads}")
    private String uploadPath;

    @GetMapping("/page")
    @Operation(summary = "分页查询文档")
    public R<PageResult<OaDocument>> page(@RequestParam int pageNum,
                                          @RequestParam int pageSize) {
        IPage<OaDocument> page = documentService.pageList(pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/upload")
    @RequireAdmin
    @Operation(summary = "上传文档")
    public R<Void> upload(@RequestParam("file") MultipartFile file,
                          @RequestParam Long uploaderId) {
        documentService.upload(file, uploaderId);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除文档")
    public R<Void> delete(@PathVariable Long id) {
        documentService.removeById(id);
        return R.ok();
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "下载文档")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        OaDocument doc = documentService.getById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(uploadPath, doc.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        String encodedName = URLEncoder.encode(doc.getDocName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }
}
