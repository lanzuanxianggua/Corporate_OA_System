package cn.oa.knowledge.service;

import cn.oa.knowledge.dto.KmCategoryCreateDTO;
import cn.oa.knowledge.entity.KmCategory;
import cn.oa.knowledge.mapper.KmCategoryMapper;
import cn.oa.knowledge.mapper.KmEntryMapper;
import cn.oa.knowledge.vo.KmCategoryVO;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class KmCategoryService {
    private final KmCategoryMapper mapper;
    private final KmEntryMapper entryMapper;

    @Transactional public Long create(KmCategoryCreateDTO dto) {
        KmCategory c = new KmCategory(); c.setCategoryName(dto.getCategoryName());
        c.setParentId(dto.getParentId()); c.setSortOrder(dto.getSortOrder()); c.setDescription(dto.getDescription());
        mapper.insert(c);
        log.info("分类创建成功: id={}, name={}", c.getId(), dto.getCategoryName());
        return c.getId();
    }

    @Transactional public void update(Long id, KmCategoryCreateDTO dto) {
        KmCategory c = mapper.selectById(id);
        if (c == null) throw new BizException(RCode.NOT_FOUND, "分类不存在");
        c.setCategoryName(dto.getCategoryName()); c.setSortOrder(dto.getSortOrder()); c.setDescription(dto.getDescription());
        mapper.updateById(c);
    }

    @Transactional public void delete(Long id) {
        if (mapper.selectCount(new LambdaQueryWrapper<KmCategory>().eq(KmCategory::getParentId, id)) > 0)
            throw new BizException(RCode.BAD_REQUEST, "请先删除子分类");
        mapper.deleteById(id);
    }

    public List<KmCategoryVO> listTree() {
        return mapper.findTree().stream().filter(c -> c.getParentId() == 0 || c.getParentId() == null)
            .map(c -> { KmCategoryVO vo = new KmCategoryVO();
                vo.setId(c.getId()); vo.setCategoryName(c.getCategoryName());
                vo.setChildren(findChildren(c.getId())); return vo; })
            .collect(Collectors.toList());
    }

    private List<KmCategoryVO> findChildren(Long parentId) {
        return mapper.findTree().stream().filter(c -> parentId.equals(c.getParentId()))
            .map(c -> { KmCategoryVO vo = new KmCategoryVO();
                vo.setId(c.getId()); vo.setCategoryName(c.getCategoryName());
                vo.setChildren(findChildren(c.getId())); return vo; })
            .collect(Collectors.toList());
    }
}