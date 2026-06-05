package cn.oa.document.service;

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
 * 档案 Service (只读).
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

    private DocArchiveVO toVO(DocArchive archive) {
        DocArchiveVO vo = new DocArchiveVO();
        vo.setId(archive.getId());
        vo.setArchiveNo(archive.getArchiveNo());
        vo.setArchiveTitle(archive.getArchiveTitle());
        vo.setDocType(archive.getDocType());
        vo.setBizId(archive.getBizId());
        vo.setRemark(archive.getRemark());
        vo.setStatus(archive.getStatus());
        vo.setCreateTime(archive.getCreateTime());
        return vo;
    }
}
