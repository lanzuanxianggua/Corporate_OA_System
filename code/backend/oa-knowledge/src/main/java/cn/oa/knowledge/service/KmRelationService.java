package cn.oa.knowledge.service;

import cn.oa.knowledge.entity.KmRelation;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 知识关联服务
 */
public interface KmRelationService extends IService<KmRelation> {

    /**
     * 添加关联
     */
    void addRelation(Long entryId, Long relatedEntryId, String relationType);

    /**
     * 移除关联
     */
    void removeRelation(Long entryId, Long relatedEntryId);

    /**
     * 获取条目的关联列表
     */
    List<KmRelation> getRelations(Long entryId);

    /**
     * 获取推荐关联条目ID列表（基于关联评分）
     */
    List<Long> getRecommendedEntryIds(Long entryId, int limit);
}
