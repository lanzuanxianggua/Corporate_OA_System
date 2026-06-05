package cn.oa.document.service;

import cn.oa.document.constant.DocConstants;
import cn.oa.document.dto.DocDispatchCreateDTO;
import cn.oa.document.dto.DocDispatchQueryDTO;
import cn.oa.document.entity.DocDispatch;
import cn.oa.document.mapper.DocDispatchMapper;
import cn.oa.document.vo.DocDispatchVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.service.WfInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 发文 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocDispatchService {

    private final DocDispatchMapper mapper;
    private final WfInstanceService wfInstanceService;

    /**
     * 创建发文 (DRAFT).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(DocDispatchCreateDTO dto, Long empId, Long deptId) {
        DocDispatch dispatch = new DocDispatch();
        dispatch.setTitle(dto.getTitle());
        dispatch.setSubjectWord(dto.getSubjectWord());
        dispatch.setSendToDept(dto.getSendToDept());
        dispatch.setCopyToDept(dto.getCopyToDept());
        dispatch.setUrgency(dto.getUrgency());
        dispatch.setSecurityLevel(dto.getSecurityLevel());
        dispatch.setContent(dto.getContent());
        dispatch.setStatus(DocConstants.DISPATCH_STATUS_DRAFT);
        dispatch.setEmpId(empId);
        dispatch.setDeptId(deptId);
        dispatch.setCreateBy(String.valueOf(empId));
        mapper.insert(dispatch);
        log.info("发文已创建: id={}, empId={}", dispatch.getId(), empId);
        return dispatch.getId();
    }

    /**
     * 提交发文 (DRAFT -> PENDING + 启动工作流).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long id, Long empId) {
        DocDispatch dispatch = mapper.selectById(id);
        if (dispatch == null) {
            throw new BizException(RCode.NOT_FOUND, "发文不存在: " + id);
        }
        if (!DocConstants.DISPATCH_STATUS_DRAFT.equals(dispatch.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅草稿状态可提交, 当前状态: " + dispatch.getStatus());
        }
        if (!Objects.equals(dispatch.getEmpId(), empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能提交自己的发文");
        }

        // 更新状态为 PENDING
        dispatch.setStatus(DocConstants.DISPATCH_STATUS_PENDING);
        mapper.updateById(dispatch);

        // 启动工作流
        String businessKey = DocConstants.BIZ_KEY_PREFIX_DISPATCH + id;
        Long wfInstanceId = wfInstanceService.start(DocConstants.WF_DEF_DISPATCH, businessKey, empId);
        dispatch.setWfInstanceId(wfInstanceId);
        mapper.updateById(dispatch);

        log.info("发文已提交审批: id={}, empId={}, wfInstanceId={}", id, empId, wfInstanceId);
        return wfInstanceId;
    }

    /**
     * 审批通过 (PENDING -> APPROVED).
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        DocDispatch dispatch = mapper.selectById(id);
        if (dispatch == null) {
            throw new BizException(RCode.NOT_FOUND, "发文不存在: " + id);
        }
        if (!DocConstants.DISPATCH_STATUS_PENDING.equals(dispatch.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅待审批状态可审批通过, 当前状态: " + dispatch.getStatus());
        }
        dispatch.setStatus(DocConstants.DISPATCH_STATUS_APPROVED);
        mapper.updateById(dispatch);
        log.info("发文审批通过: id={}", id);
    }

    /**
     * 发布 (APPROVED -> PUBLISHED).
     */
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        DocDispatch dispatch = mapper.selectById(id);
        if (dispatch == null) {
            throw new BizException(RCode.NOT_FOUND, "发文不存在: " + id);
        }
        if (!DocConstants.DISPATCH_STATUS_APPROVED.equals(dispatch.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅已审批状态可发布, 当前状态: " + dispatch.getStatus());
        }
        dispatch.setStatus(DocConstants.DISPATCH_STATUS_PUBLISHED);
        mapper.updateById(dispatch);
        log.info("发文已发布: id={}", id);
    }

    /**
     * 归档 (PUBLISHED -> ARCHIVED).
     */
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long id) {
        DocDispatch dispatch = mapper.selectById(id);
        if (dispatch == null) {
            throw new BizException(RCode.NOT_FOUND, "发文不存在: " + id);
        }
        if (!DocConstants.DISPATCH_STATUS_PUBLISHED.equals(dispatch.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅已发布状态可归档, 当前状态: " + dispatch.getStatus());
        }
        dispatch.setStatus(DocConstants.DISPATCH_STATUS_ARCHIVED);
        mapper.updateById(dispatch);
        log.info("发文已归档: id={}", id);
    }

    /**
     * 更新发文 (仅 DRAFT 可更新).
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DocDispatchCreateDTO dto, Long empId) {
        DocDispatch dispatch = mapper.selectById(id);
        if (dispatch == null) {
            throw new BizException(RCode.NOT_FOUND, "发文不存在: " + id);
        }
        if (!DocConstants.DISPATCH_STATUS_DRAFT.equals(dispatch.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅草稿状态可更新, 当前状态: " + dispatch.getStatus());
        }
        if (!Objects.equals(dispatch.getEmpId(), empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能更新自己的发文");
        }

        dispatch.setTitle(dto.getTitle());
        dispatch.setSubjectWord(dto.getSubjectWord());
        dispatch.setSendToDept(dto.getSendToDept());
        dispatch.setCopyToDept(dto.getCopyToDept());
        dispatch.setUrgency(dto.getUrgency());
        dispatch.setSecurityLevel(dto.getSecurityLevel());
        dispatch.setContent(dto.getContent());
        mapper.updateById(dispatch);
        log.info("发文已更新: id={}", id);
    }

    /**
     * 删除发文 (软删除).
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long empId) {
        DocDispatch dispatch = mapper.selectById(id);
        if (dispatch == null) {
            throw new BizException(RCode.NOT_FOUND, "发文不存在: " + id);
        }
        if (!DocConstants.DISPATCH_STATUS_DRAFT.equals(dispatch.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅草稿状态可删除, 当前状态: " + dispatch.getStatus());
        }
        if (!Objects.equals(dispatch.getEmpId(), empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能删除自己的发文");
        }
        mapper.deleteById(id);
        log.info("发文已删除: id={}", id);
    }

    /**
     * 发文详情.
     */
    public DocDispatchVO getById(Long id) {
        Map<String, Object> detail = mapper.findDetail(id);
        if (detail == null) {
            throw new BizException(RCode.NOT_FOUND, "发文不存在: " + id);
        }
        return mapToVO(detail);
    }

    /**
     * 分页查询发文列表.
     */
    public PageResult<DocDispatchVO> listPage(DocDispatchQueryDTO query, Long deptId) {
        Page<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<Map<String, Object>> result = mapper.findPageWithJoins(page, query.getStatus(), query.getKeyword(), deptId);

        List<DocDispatchVO> voList = result.getRecords().stream()
                .map(this::mapToVO)
                .toList();

        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    private DocDispatchVO mapToVO(Map<String, Object> map) {
        if (map == null) return null;
        DocDispatchVO vo = new DocDispatchVO();
        vo.setId(toLong(map.get("id")));
        vo.setTitle(toStr(map.get("title")));
        vo.setSubjectWord(toStr(map.get("subject_word")));
        vo.setSendToDept(toStr(map.get("send_to_dept")));
        vo.setCopyToDept(toStr(map.get("copy_to_dept")));
        vo.setUrgency(toStr(map.get("urgency")));
        vo.setSecurityLevel(toStr(map.get("security_level")));
        vo.setContent(toStr(map.get("content")));
        vo.setStatus(toStr(map.get("status")));
        vo.setEmpId(toLong(map.get("emp_id")));
        vo.setEmpName(toStr(map.get("emp_name")));
        vo.setDeptId(toLong(map.get("dept_id")));
        vo.setDeptName(toStr(map.get("dept_name")));
        vo.setWfInstanceId(toLong(map.get("wf_instance_id")));
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

    private static java.time.LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof java.time.LocalDateTime ldt) return ldt;
        if (obj instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        try { return java.time.LocalDateTime.parse(obj.toString()); } catch (Exception e) { return null; }
    }
}
