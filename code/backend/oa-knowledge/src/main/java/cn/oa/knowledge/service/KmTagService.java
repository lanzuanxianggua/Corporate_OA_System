package cn.oa.knowledge.service;

import cn.oa.knowledge.entity.KmTag;
import cn.oa.knowledge.vo.KmTagVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 知识标签服务
 */
public interface KmTagService extends IService<KmTag> {

    /**
     * 为条目批量设置标签
     */
    void setTags(Long entryId, List<String> tagNames);

    /**
     * 获取条目的标签列表
     */
    List<KmTag> getTagsByEntryId(Long entryId);

    /**
     * 删除条目的所有标签
     */
    void removeByEntryId(Long entryId);

    /**
     * 获取热门标签
     */
    List<KmTagVO> getHotTags(int limit);

    /**
     * 根据标签名称搜索
     */
    List<KmTag> searchByTagName(String keyword);
}
