package cn.oa.knowledge.service;

import cn.oa.knowledge.dto.KmEntryCreateDTO;
import cn.oa.knowledge.dto.KmEntryQueryDTO;
import cn.oa.knowledge.dto.KmEntryUpdateDTO;
import cn.oa.knowledge.dto.KmVersionCreateDTO;
import cn.oa.knowledge.entity.KmEntry;
import cn.oa.knowledge.entity.KmVersion;
import cn.oa.knowledge.vo.KmEntryVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识条目服务
 */
public interface KmEntryService extends IService<KmEntry> {

    /**
     * 分页查询知识条目
     */
    IPage<KmEntryVO> pageQuery(KmEntryQueryDTO queryDTO);

    /**
     * 创建知识条目
     */
    KmEntry create(KmEntryCreateDTO createDTO, Long createById);

    /**
     * 更新知识条目
     */
    KmEntry update(KmEntryUpdateDTO updateDTO);

    /**
     * 发布知识条目
     */
    void publish(Long id);

    /**
     * 归档知识条目
     */
    void archive(Long id);

    /**
     * 获取知识条目详情（含标签）
     */
    KmEntryVO getDetail(Long id);

    /**
     * 上传附件并创建新版本
     */
    KmVersion uploadVersion(KmVersionCreateDTO createDTO, MultipartFile file, Long uploaderId);

    /**
     * 获取条目的版本列表
     */
    List<KmVersion> getVersions(Long entryId);

    /**
     * 增加浏览次数
     */
    void incrementViewCount(Long id);

    /**
     * 增加下载次数
     */
    void incrementDownloadCount(Long id);
}
