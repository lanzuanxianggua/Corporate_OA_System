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
import java.util.UUID;

@Service
public class DocumentServiceImpl extends ServiceImpl<OaDocumentMapper, OaDocument> implements DocumentService {

    @Value("${oa.upload.path:uploads}")
    private String uploadBasePath;

    @Override
    public void upload(MultipartFile file, Long uploaderId) {
        String originalFilename = file.getOriginalFilename();
        String fileType = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String fileName = UUID.randomUUID().toString() + fileType;

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
