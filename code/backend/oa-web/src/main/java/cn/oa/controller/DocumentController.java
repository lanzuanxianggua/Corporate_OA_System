package cn.oa.controller;

import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaDocument;
import cn.oa.service.DocumentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/document")
@Tag(name = "文档管理")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @GetMapping("/page")
    @Operation(summary = "分页查询文档")
    public R<PageResult<OaDocument>> page(@RequestParam int pageNum,
                                          @RequestParam int pageSize) {
        IPage<OaDocument> page = documentService.pageList(pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文档")
    public R<Void> upload(@RequestParam("file") MultipartFile file,
                          @RequestParam Long uploaderId) {
        documentService.upload(file, uploaderId);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档")
    public R<Void> delete(@PathVariable Long id) {
        documentService.removeById(id);
        return R.ok();
    }
}
