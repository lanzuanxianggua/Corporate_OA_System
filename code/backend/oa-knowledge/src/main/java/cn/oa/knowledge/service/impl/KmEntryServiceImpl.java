package cn.oa.knowledge.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.knowledge.dto.KmEntryCreateDTO;
import cn.oa.knowledge.dto.KmEntryQueryDTO;
import cn.oa.knowledge.dto.KmEntryUpdateDTO;
import cn.oa.knowledge.dto.KmVersionCreateDTO;
import cn.oa.knowledge.entity.KmEntry;
import cn.oa.knowledge.entity.KmTag;
import cn.oa.knowledge.entity.KmVersion;
import cn.oa.knowledge.mapper.KmEntryMapper;
import cn.oa.knowledge.mapper.KmTagMapper;
import cn.oa.knowledge.mapper.KmVersionMapper;
import cn.oa.knowledge.service.KmEntryService;
import cn.oa.knowledge.service.KmTagService;
import cn.oa.knowledge.vo.KmEntryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识条目服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KmEntryServiceImpl extends ServiceImpl<KmEntryMapper, KmEntry> implements KmEntryService {

    private final KmTagMapper kmTagMapper;
    private final KmVersionMapper kmVersionMapper;
    private final KmTagService kmTagService;

    @Override
    public IPage<KmEntryVO> pageQuery(KmEntryQueryDTO queryDTO) {
        Page<KmEntry> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<KmEntry> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                    .like(KmEntry::getTitle, queryDTO.getKeyword())
                    .or()
                    .like(KmEntry::getContent, queryDTO.getKeyword()));
        }
        if (StringUtils.isNotBlank(queryDTO.getStatus())) {
            wrapper.eq(KmEntry::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getDeptId() != null) {
            wrapper.eq(KmEntry::getDeptId, queryDTO.getDeptId());
        }
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(KmEntry::getCategoryId, queryDTO.getCategoryId());
        }
        if (StringUtils.isNotBlank(queryDTO.getSecurityLevel())) {
            wrapper.eq(KmEntry::getSecurityLevel, queryDTO.getSecurityLevel());
        }
        wrapper.orderByDesc(KmEntry::getUpdateTime);

        IPage<KmEntry> pageResult = this.page(page, wrapper);

        // Convert to VO with tags
        Page<KmEntryVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        List<KmEntryVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmEntry create(KmEntryCreateDTO createDTO, Long createById) {
        KmEntry entry = new KmEntry();
        entry.setTitle(createDTO.getTitle());
        entry.setContent(createDTO.getContent());
        entry.setStatus("DRAFT");
        entry.setDeptId(createDTO.getDeptId());
        entry.setSecurityLevel(createDTO.getSecurityLevel() != null ? createDTO.getSecurityLevel() : "PUBLIC");
        entry.setCategoryId(createDTO.getCategoryId());
        entry.setCurrentVersion(0);
        entry.setViewCount(0);
        entry.setDownloadCount(0);
        this.save(entry);

        // Set tags
        if (createDTO.getTags() != null && !createDTO.getTags().isEmpty()) {
            kmTagService.setTags(entry.getId(), createDTO.getTags());
        }

        log.info("Knowledge entry created: id={}, title={}, createById={}", entry.getId(), entry.getTitle(), createById);
        return entry;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmEntry update(KmEntryUpdateDTO updateDTO) {
        KmEntry existing = this.getById(updateDTO.getId());
        if (existing == null) {
            throw new BusinessException("知识条目不存在");
        }

        existing.setTitle(updateDTO.getTitle());
        existing.setContent(updateDTO.getContent());
        existing.setDeptId(updateDTO.getDeptId());
        existing.setSecurityLevel(updateDTO.getSecurityLevel());
        existing.setCategoryId(updateDTO.getCategoryId());
        this.updateById(existing);

        // Update tags
        if (updateDTO.getTags() != null) {
            kmTagService.setTags(existing.getId(), updateDTO.getTags());
        }

        log.info("Knowledge entry updated: id={}", existing.getId());
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        KmEntry entry = this.getById(id);
        if (entry == null) {
            throw new BusinessException("知识条目不存在");
        }
        if (!"DRAFT".equals(entry.getStatus())) {
            throw new BusinessException("只有草稿状态才能发布");
        }
        entry.setStatus("PUBLISHED");
        this.updateById(entry);
        log.info("Knowledge entry published: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long id) {
        KmEntry entry = this.getById(id);
        if (entry == null) {
            throw new BusinessException("知识条目不存在");
        }
        if (!"PUBLISHED".equals(entry.getStatus())) {
            throw new BusinessException("只有已发布状态才能归档");
        }
        entry.setStatus("ARCHIVED");
        this.updateById(entry);
        log.info("Knowledge entry archived: id={}", id);
    }

    @Override
    public KmEntryVO getDetail(Long id) {
        KmEntry entry = this.getById(id);
        if (entry == null) {
            throw new BusinessException("知识条目不存在");
        }
        return convertToVO(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmVersion uploadVersion(KmVersionCreateDTO createDTO, MultipartFile file, Long uploaderId) {
        KmEntry entry = this.getById(createDTO.getEntryId());
        if (entry == null) {
            throw new BusinessException("知识条目不存在");
        }

        // Determine next version number
        int nextVersion = (entry.getCurrentVersion() != null ? entry.getCurrentVersion() : 0) + 1;

        KmVersion version = new KmVersion();
        version.setEntryId(createDTO.getEntryId());
        version.setVersionNo(nextVersion);
        version.setComment(createDTO.getComment());
        version.setUploaderId(uploaderId);
        version.setUploadTime(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            version.setFilePath(file.getOriginalFilename());
            version.setFileSize(file.getSize());
            version.setFileType(file.getContentType());
        }

        kmVersionMapper.insert(version);

        // Update entry's current version
        entry.setCurrentVersion(nextVersion);
        this.updateById(entry);

        log.info("Version uploaded: entryId={}, versionNo={}, uploaderId={}",
                createDTO.getEntryId(), nextVersion, uploaderId);
        return version;
    }

    @Override
    public List<KmVersion> getVersions(Long entryId) {
        LambdaQueryWrapper<KmVersion> wrapper = new LambdaQueryWrapper<KmVersion>()
                .eq(KmVersion::getEntryId, entryId)
                .orderByDesc(KmVersion::getVersionNo);
        return kmVersionMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long id) {
        this.lambdaUpdate()
                .setSql("view_count = view_count + 1")
                .eq(KmEntry::getId, id)
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementDownloadCount(Long id) {
        this.lambdaUpdate()
                .setSql("download_count = download_count + 1")
                .eq(KmEntry::getId, id)
                .update();
    }

    /**
     * 将实体转换为VO，填充标签列表
     */
    private KmEntryVO convertToVO(KmEntry entry) {
        KmEntryVO vo = new KmEntryVO();
        vo.setId(entry.getId());
        vo.setTitle(entry.getTitle());
        vo.setContent(entry.getContent());
        vo.setCurrentVersion(entry.getCurrentVersion());
        vo.setStatus(entry.getStatus());
        vo.setDeptId(entry.getDeptId());
        vo.setSecurityLevel(entry.getSecurityLevel());
        vo.setCategoryId(entry.getCategoryId());
        vo.setViewCount(entry.getViewCount());
        vo.setDownloadCount(entry.getDownloadCount());
        vo.setCreateBy(entry.getCreateBy());
        vo.setCreateTime(entry.getCreateTime());
        vo.setUpdateBy(entry.getUpdateBy());
        vo.setUpdateTime(entry.getUpdateTime());

        // Fill tags
        List<KmTag> tags = kmTagService.getTagsByEntryId(entry.getId());
        vo.setTags(tags.stream().map(KmTag::getTagName).toList());

        return vo;
    }
}
