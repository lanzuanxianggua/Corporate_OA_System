package cn.oa.document.service;

import cn.oa.document.constant.DocConstants;
import cn.oa.document.dto.DocSignReportCreateDTO;
import cn.oa.document.dto.DocSignReportQueryDTO;
import cn.oa.document.entity.DocSignReport;
import cn.oa.document.mapper.DocSignReportMapper;
import cn.oa.document.vo.DocSignReportVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.service.WfInstanceService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 签报 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocSignReportService {

    private final DocSignReportMapper mapper;
    private final WfInstanceService wfInstanceService;

    /**
     * 创建签报 (DRAFT).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(DocSignReportCreateDTO dto, Long empId, Long deptId) {
        DocSignReport report = new DocSignReport();
        report.setTitle(dto.getTitle());
        report.setReportType(dto.getReportType());
        report.setContent(dto.getContent());
        report.setStatus(DocConstants.SIGN_REPORT_STATUS_DRAFT);
        report.setEmpId(empId);
        report.setDeptId(deptId);
        report.setCreateBy(String.valueOf(empId));
        mapper.insert(report);
        log.info("签报已创建: id={}, empId={}", report.getId(), empId);
        return report.getId();
    }

    /**
     * 提交签报 (DRAFT -> PENDING + 启动工作流).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long id, Long empId) {
        DocSignReport report = mapper.selectById(id);
        if (report == null) {
            throw new BizException(RCode.NOT_FOUND, "签报不存在: " + id);
        }
        if (!DocConstants.SIGN_REPORT_STATUS_DRAFT.equals(report.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅草稿状态可提交, 当前状态: " + report.getStatus());
        }
        if (!Objects.equals(report.getEmpId(), empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能提交自己的签报");
        }

        // 更新状态为 PENDING
        report.setStatus(DocConstants.SIGN_REPORT_STATUS_PENDING);
        mapper.updateById(report);

        // 启动工作流
        String businessKey = DocConstants.BIZ_KEY_PREFIX_SIGN_REPORT + id;
        Long wfInstanceId = wfInstanceService.start(DocConstants.WF_DEF_SIGN_REPORT, businessKey, empId);
        report.setWfInstanceId(wfInstanceId);
        mapper.updateById(report);

        log.info("签报已提交审批: id={}, empId={}, wfInstanceId={}", id, empId, wfInstanceId);
        return wfInstanceId;
    }

    /**
     * 审批通过签报 (PENDING -> APPROVED).
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        DocSignReport report = mapper.selectById(id);
        if (report == null) {
            throw new BizException(RCode.NOT_FOUND, "签报不存在: " + id);
        }
        if (!DocConstants.SIGN_REPORT_STATUS_PENDING.equals(report.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅待审批状态可审批通过, 当前状态: " + report.getStatus());
        }
        report.setStatus(DocConstants.SIGN_REPORT_STATUS_APPROVED);
        mapper.updateById(report);
        log.info("签报已审批通过: id={}", id);
    }

    /**
     * 驳回签报 (PENDING -> REJECTED).
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id) {
        DocSignReport report = mapper.selectById(id);
        if (report == null) {
            throw new BizException(RCode.NOT_FOUND, "签报不存在: " + id);
        }
        if (!DocConstants.SIGN_REPORT_STATUS_PENDING.equals(report.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅待审批状态可驳回, 当前状态: " + report.getStatus());
        }
        report.setStatus(DocConstants.SIGN_REPORT_STATUS_REJECTED);
        mapper.updateById(report);
        log.info("签报已驳回: id={}", id);
    }

    /**
     * 签报详情.
     */
    public DocSignReportVO getById(Long id) {
        Map<String, Object> detail = mapper.findDetail(id);
        if (detail == null) {
            throw new BizException(RCode.NOT_FOUND, "签报不存在: " + id);
        }
        return mapToVO(detail);
    }

    /**
     * 分页查询签报列表.
     */
    public PageResult<DocSignReportVO> listPage(DocSignReportQueryDTO query, Long empId) {
        Page<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<Map<String, Object>> result = mapper.findPageWithJoins(page, query.getStatus(), null, empId);

        List<DocSignReportVO> voList = result.getRecords().stream()
                .map(this::mapToVO)
                .toList();

        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    private DocSignReportVO mapToVO(Map<String, Object> map) {
        if (map == null) return null;
        DocSignReportVO vo = new DocSignReportVO();
        vo.setId(toLong(map.get("id")));
        vo.setTitle(toStr(map.get("title")));
        vo.setReportType(toStr(map.get("report_type")));
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
