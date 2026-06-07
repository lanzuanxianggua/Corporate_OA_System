package cn.oa.document.service;

import cn.oa.document.constant.DocConstants;
import cn.oa.document.entity.DocArchive;
import cn.oa.document.mapper.DocArchiveMapper;
import cn.oa.document.vo.DocArchiveVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 档案 Service.
 *
 * <p>v2 设计: 档案数据由发文/收文/签报归档动作写入, 本服务只读 + 按需手动新增.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocArchiveService {

    private final DocArchiveMapper mapper;

    /**
     * 档案详情.
     */
    public DocArchiveVO getById(Long id) {
        DocArchive archive = mapper.selectById(id);
        if (archive == null) {
            throw new BizException(RCode.NOT_FOUND, "档案不存在: " + id);
        }
        return toVO(archive);
    }

    /**
     * 分页查询档案列表.
     */
    public PageResult<DocArchiveVO> listPage(int pageNum, int pageSize) {
        Page<DocArchive> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DocArchive> wrapper = new LambdaQueryWrapper<DocArchive>()
                .orderByDesc(DocArchive::getCreateTime);

        Page<DocArchive> result = mapper.selectPage(page, wrapper);
        List<DocArchiveVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, result.getTotal(), pageNum, pageSize);
    }

    /**
     * 新增档案 (供其他业务模块回调使用).
     */
    public Long create(DocArchive archive) {
        if (archive.getStatus() == null) {
            archive.setStatus(DocConstants.ARCHIVE_STATUS_ACTIVE);
        }
        mapper.insert(archive);
        log.info("档案已创建: id={}, type={}, sourceId={}",
                archive.getId(), archive.getArchiveType(), archive.getSourceId());
        return archive.getId();
    }

    private DocArchiveVO toVO(DocArchive archive) {
        DocArchiveVO vo = new DocArchiveVO();
        vo.setId(archive.getId());
        vo.setArchiveNo(archive.getArchiveNo());
        vo.setArchiveType(archive.getArchiveType());
        vo.setSourceId(archive.getSourceId());
        vo.setArchiveDate(archive.getArchiveDate());
        vo.setTitle(archive.getTitle());
        vo.setStatus(archive.getStatus());
        vo.setCreateTime(archive.getCreateTime());
        vo.setUpdateTime(archive.getUpdateTime());
        return vo;
    }
}
