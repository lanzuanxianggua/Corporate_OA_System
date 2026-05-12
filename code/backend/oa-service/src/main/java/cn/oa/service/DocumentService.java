package cn.oa.service;

import cn.oa.entity.OaDocument;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService extends IService<OaDocument> {

    /**
     * 上传文档
     */
    void upload(MultipartFile file, Long uploaderId);

    /**
     * 分页查询文档列表
     */
    IPage<OaDocument> pageList(int pageNum, int pageSize);
}
