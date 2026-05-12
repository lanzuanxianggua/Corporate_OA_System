package cn.oa.service.impl;

import cn.oa.entity.OaDocument;
import cn.oa.mapper.OaDocumentMapper;
import cn.oa.service.DocumentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentServiceImpl extends ServiceImpl<OaDocumentMapper, OaDocument> implements DocumentService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void upload(MultipartFile file, Long uploaderId) {
        // 获取文件信息
        String originalFilename = file.getOriginalFilename();
        String fileType = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String fileName = UUID.randomUUID().toString() + fileType;

        // 保存文件到服务器
        String filePath = "uploads/" + fileName;
        File dest = new File(filePath);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }

        // 保存文档记录
        OaDocument document = new OaDocument();
        document.setDocName(originalFilename);
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setFileType(fileType);
        document.setUploaderId(uploaderId);
        this.save(document);
    }

    @Override
    public IPage<OaDocument> pageList(int pageNum, int pageSize) {
        Page<OaDocument> page = new Page<>(pageNum, pageSize);
        return this.page(page);
    }
}
