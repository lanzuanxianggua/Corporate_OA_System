package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaDocument;
import cn.oa.mapper.OaDocumentMapper;
import cn.oa.service.DocumentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentServiceImpl extends ServiceImpl<OaDocumentMapper, OaDocument> implements DocumentService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".csv",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".zip", ".rar", ".7z"
    );

    @Value("${oa.upload.path:uploads}")
    private String uploadBasePath;

    @Override
    public void upload(MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过20MB");
        }

        String originalFilename = file.getOriginalFilename();
        // 路径穿越防护：只保留文件名部分
        if (originalFilename != null) {
            originalFilename = Paths.get(originalFilename).getFileName().toString();
        }

        String fileType = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileType = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(fileType)) {
            throw new BusinessException("不支持的文件类型: " + fileType + "，允许的类型: " + ALLOWED_EXTENSIONS);
        }

        String fileName = UUID.randomUUID() + fileType;

        File uploadDir = new File(uploadBasePath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        File dest = new File(uploadDir, fileName);
        try {
            file.transferTo(dest.getAbsoluteFile());
        } catch (IOException e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        OaDocument document = new OaDocument();
        document.setDocName(originalFilename);
        document.setFilePath(fileName);
        document.setFileSize(file.getSize());
        document.setFileType(fileType);
        document.setUploaderId(uploaderId);
        this.save(document);
    }

    @Override
    public IPage<OaDocument> pageList(int pageNum, int pageSize, String keyword) {
        Page<OaDocument> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaDocument> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(OaDocument::getDocName, keyword);
        }
        wrapper.orderByDesc(OaDocument::getCreateTime);
        return this.page(page, wrapper);
    }
}
