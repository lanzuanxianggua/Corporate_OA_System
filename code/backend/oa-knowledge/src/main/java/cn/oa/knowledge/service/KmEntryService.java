package cn.oa.knowledge.service;

import cn.oa.knowledge.dto.KmEntryCreateDTO;
import cn.oa.knowledge.dto.KmEntryQueryDTO;
import cn.oa.knowledge.entity.KmEntry;
import cn.oa.knowledge.entity.KmVersion;
import cn.oa.knowledge.mapper.KmCategoryMapper;
import cn.oa.knowledge.mapper.KmEntryMapper;
import cn.oa.knowledge.mapper.KmVersionMapper;
import cn.oa.knowledge.vo.KmEntryVO;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class KmEntryService {
    private final KmEntryMapper mapper;
    private final KmVersionMapper versionMapper;
    private final KmCategoryMapper categoryMapper;

    @Transactional public Long create(KmEntryCreateDTO dto, Long empId) {
        KmEntry e = new KmEntry(); e.setTitle(dto.getTitle()); e.setContent(dto.getContent());
        e.setSummary(dto.getSummary()); e.setCategoryId(dto.getCategoryId()); e.setTags(dto.getTags());
        e.setStatus("DRAFT"); e.setViewCount(0); e.setCreateEmpId(empId);
        mapper.insert(e);
        KmVersion v = new KmVersion(); v.setEntryId(e.getId()); v.setVersionNo(1);
        v.setTitle(dto.getTitle()); v.setContent(dto.getContent()); v.setSummary(dto.getSummary()); v.setCreateEmpId(empId);
        versionMapper.insert(v);
        log.info("知识条目创建成功: id={}, title={}", e.getId(), dto.getTitle());
        return e.getId();
    }

    @Transactional public void update(Long id, KmEntryCreateDTO dto, Long empId) {
        KmEntry e = mapper.selectById(id);
        if (e == null) throw new BizException(RCode.NOT_FOUND, "条目不存在");
        e.setTitle(dto.getTitle()); e.setContent(dto.getContent()); e.setSummary(dto.getSummary());
        e.setCategoryId(dto.getCategoryId()); e.setTags(dto.getTags());
        mapper.updateById(e);
        List<KmVersion> vs = versionMapper.findByEntryIdOrderByVersion(id);
        int newVer = vs.isEmpty() ? 1 : vs.get(0).getVersionNo() + 1;
        KmVersion v = new KmVersion(); v.setEntryId(id); v.setVersionNo(newVer);
        v.setTitle(dto.getTitle()); v.setContent(dto.getContent()); v.setSummary(dto.getSummary()); v.setCreateEmpId(empId);
        versionMapper.insert(v);
    }

    @Transactional public void publish(Long id) { changeStatus(id, "PUBLISHED"); }
    @Transactional public void archive(Long id) { changeStatus(id, "ARCHIVED"); }
    @Transactional public void delete(Long id) { mapper.deleteById(id); }

    private void changeStatus(Long id, String st) {
        KmEntry e = mapper.selectById(id);
        if (e == null) throw new BizException(RCode.NOT_FOUND, "条目不存在");
        e.setStatus(st); mapper.updateById(e);
    }

    public KmEntryVO getById(Long id) {
        KmEntry e = mapper.selectById(id);
        if (e == null) throw new BizException(RCode.NOT_FOUND, "条目不存在");
        e.setViewCount(e.getViewCount() == null ? 1 : e.getViewCount() + 1);
        mapper.updateById(e);
        KmEntryVO vo = new KmEntryVO(); vo.setId(e.getId()); vo.setTitle(e.getTitle());
        vo.setContent(e.getContent()); vo.setSummary(e.getSummary()); vo.setCategoryId(e.getCategoryId());
        vo.setTags(e.getTags()); vo.setStatus(e.getStatus()); vo.setViewCount(e.getViewCount());
        vo.setCreateTime(e.getCreateTime());
        if (e.getCategoryId() != null) {
            var cat = categoryMapper.selectById(e.getCategoryId());
            if (cat != null) vo.setCategoryName(cat.getCategoryName());
        }
        var vs = versionMapper.findByEntryIdOrderByVersion(id);
        if (!vs.isEmpty()) vo.setVersionNo(vs.get(0).getVersionNo());
        return vo;
    }

    public Page<KmEntry> listPage(KmEntryQueryDTO query) {
        Page<KmEntry> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<KmEntry> w = new LambdaQueryWrapper<KmEntry>()
                .eq(query.getCategoryId() != null, KmEntry::getCategoryId, query.getCategoryId())
                .eq(query.getStatus() != null, KmEntry::getStatus, query.getStatus())
                .orderByDesc(KmEntry::getCreateTime);
        return mapper.selectPage(page, w);
    }

    public List<KmEntry> search(String keyword, int limit) {
        return mapper.searchByKeyword(keyword, limit);
    }
}