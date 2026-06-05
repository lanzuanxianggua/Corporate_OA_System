package cn.oa.document.service;

import cn.oa.document.constant.DocConstants;
import cn.oa.document.dto.DocReceiveCreateDTO;
import cn.oa.document.dto.DocReceiveQueryDTO;
import cn.oa.document.entity.DocReceive;
import cn.oa.document.mapper.DocReceiveMapper;
import cn.oa.document.vo.DocReceiveVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 收文 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocReceiveService {

    private final DocReceiveMapper mapper;

    /**
     * 登记收文 (PENDING).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(DocReceiveCreateDTO dto, Long deptId) {
        DocReceive receive = new DocReceive();
        receive.setSourceDept(dto.getSourceDept());
        receive.setDocTitle(dto.getDocTitle());
        receive.setDocDate(dto.getDocDate());
        receive.setReceiveDate(dto.getReceiveDate());
        receive.setUrgentLevel(dto.getUrgentLevel());
        receive.setContent(dto.getContent());
        receive.setProcessOpinion(dto.getProcessOpinion());
        receive.setStatus(DocConstants.RECEIVE_STATUS_PENDING);
        receive.setProcessDeptId(deptId);
        mapper.insert(receive);
        log.info("收文已登记: id={}", receive.getId());
        return receive.getId();
    }

    /**
     * 更新收文信息.
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DocReceiveCreateDTO dto) {
        DocReceive receive = mapper.selectById(id);
        if (receive == null) {
            throw new BizException(RCode.NOT_FOUND, "收文不存在: " + id);
        }
        receive.setSourceDept(dto.getSourceDept());
        receive.setDocTitle(dto.getDocTitle());
        receive.setDocDate(dto.getDocDate());
        receive.setReceiveDate(dto.getReceiveDate());
        receive.setUrgentLevel(dto.getUrgentLevel());
        receive.setContent(dto.getContent());
        receive.setProcessOpinion(dto.getProcessOpinion());
        mapper.updateById(receive);
        log.info("收文已更新: id={}", id);
    }

    /**
     * 归档收文 (COMPLETED -> ARCHIVED).
     */
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long id) {
        DocReceive receive = mapper.selectById(id);
        if (receive == null) {
            throw new BizException(RCode.NOT_FOUND, "收文不存在: " + id);
        }
        if (!DocConstants.RECEIVE_STATUS_COMPLETED.equals(receive.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅已办结状态可归档, 当前状态: " + receive.getStatus());
        }
        receive.setStatus(DocConstants.RECEIVE_STATUS_ARCHIVED);
        mapper.updateById(receive);
        log.info("收文已归档: id={}", id);
    }

    /**
     * 收文详情.
     */
    public DocReceiveVO getById(Long id) {
        Map<String, Object> detail = mapper.findDetail(id);
        if (detail == null) {
            throw new BizException(RCode.NOT_FOUND, "收文不存在: " + id);
        }
        return mapToVO(detail);
    }

    /**
     * 分页查询收文列表.
     */
    public PageResult<DocReceiveVO> listPage(DocReceiveQueryDTO query, Long deptId) {
        Page<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<Map<String, Object>> result = mapper.findPageWithJoins(page, query.getStatus(), query.getKeyword(), deptId);

        List<DocReceiveVO> voList = result.getRecords().stream()
                .map(this::mapToVO)
                .toList();

        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    private DocReceiveVO mapToVO(Map<String, Object> map) {
        if (map == null) return null;
        DocReceiveVO vo = new DocReceiveVO();
        vo.setId(toLong(map.get("id")));
        vo.setSourceDept(toStr(map.get("source_dept")));
        vo.setDocTitle(toStr(map.get("doc_title")));
        vo.setDocDate(toLocalDate(map.get("doc_date")));
        vo.setReceiveDate(toLocalDate(map.get("receive_date")));
        vo.setUrgentLevel(toStr(map.get("urgent_level")));
        vo.setContent(toStr(map.get("content")));
        vo.setProcessOpinion(toStr(map.get("process_opinion")));
        vo.setStatus(toStr(map.get("status")));
        vo.setProcessDeptId(toLong(map.get("process_dept_id")));
        vo.setProcessDeptName(toStr(map.get("process_dept_name")));
        vo.setCreateTime(toLocalDateTime(map.get("create_time")));
        vo.setUpdateTime(toLocalDateTime(map.get("update_time")));
        return vo;
    }

    private static Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long l) return l;
        if (obj instanceof Number n) return n.longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return null; }
    }

    private static String toStr(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private static java.time.LocalDate toLocalDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof java.time.LocalDate ld) return ld;
        if (obj instanceof java.sql.Date sd) return sd.toLocalDate();
        try { return java.time.LocalDate.parse(obj.toString()); } catch (Exception e) { return null; }
    }

    private static java.time.LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof java.time.LocalDateTime ldt) return ldt;
        if (obj instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        try { return java.time.LocalDateTime.parse(obj.toString()); } catch (Exception e) { return null; }
    }
}
