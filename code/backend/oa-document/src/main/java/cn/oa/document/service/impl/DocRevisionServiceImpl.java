package cn.oa.document.service.impl;

import cn.oa.document.dto.DocRevisionCreateDTO;
import cn.oa.document.entity.DocRevision;
import cn.oa.document.mapper.DocRevisionMapper;
import cn.oa.document.service.DocRevisionService;
import cn.oa.document.vo.DocRevisionVO;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公文修订版本服务实现
 *
 * @author oa-document
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocRevisionServiceImpl extends ServiceImpl<DocRevisionMapper, DocRevision> implements DocRevisionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addRevision(DocRevisionCreateDTO dto, Long editorId) {
        // 获取最大版本号
        LambdaQueryWrapper<DocRevision> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocRevision::getDispatchId, dto.getDispatchId())
                .orderByDesc(DocRevision::getVersionNo)
                .last("LIMIT 1");
        DocRevision last = baseMapper.selectOne(wrapper);
        int nextVersion = (last != null) ? last.getVersionNo() + 1 : 1;

        DocRevision revision = new DocRevision();
        revision.setDispatchId(dto.getDispatchId());
        revision.setVersionNo(nextVersion);
        revision.setContent(dto.getContent());
        revision.setEditorId(editorId);
        revision.setEditTime(LocalDateTime.now());
        revision.setComment(dto.getComment());
        revision.setIsClean(0); // 默认非清稿
        baseMapper.insert(revision);
        log.info("公文版本已创建: dispatchId={}, versionNo={}", dto.getDispatchId(), nextVersion);
        return revision.getId();
    }

    @Override
    public List<DocRevisionVO> getHistory(Long dispatchId) {
        LambdaQueryWrapper<DocRevision> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocRevision::getDispatchId, dispatchId)
                .orderByDesc(DocRevision::getVersionNo);
        List<DocRevision> list = baseMapper.selectList(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clean(Long id) {
        DocRevision revision = baseMapper.selectById(id);
        if (revision == null) {
            throw new BusinessException("版本不存在");
        }
        revision.setIsClean(1);
        baseMapper.updateById(revision);
        log.info("公文版本已清稿: id={}", id);
    }

    /**
     * Entity 转 VO
     */
    private DocRevisionVO toVO(DocRevision entity) {
        if (entity == null) {
            return null;
        }
        DocRevisionVO vo = new DocRevisionVO();
        vo.setId(entity.getId());
        vo.setDispatchId(entity.getDispatchId());
        vo.setVersionNo(entity.getVersionNo());
        vo.setContent(entity.getContent());
        vo.setEditorId(entity.getEditorId());
        vo.setEditTime(entity.getEditTime());
        vo.setComment(entity.getComment());
        vo.setIsClean(entity.getIsClean());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
