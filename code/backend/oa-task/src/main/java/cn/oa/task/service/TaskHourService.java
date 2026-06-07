package cn.oa.task.service;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.task.dto.TaskHourCreateDTO;
import cn.oa.task.entity.TaskHour;
import cn.oa.task.mapper.TaskHourMapper;
import cn.oa.task.vo.TaskHourVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工时 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskHourService {

    private final TaskHourMapper mapper;

    @Transactional
    public Long create(TaskHourCreateDTO dto, Long empId) {
        if (dto.getItemId() == null) {
            throw new BizException(RCode.BAD_REQUEST, "itemId 不能为空");
        }
        if (dto.getHours() == null || dto.getHours().compareTo(BigDecimal.ZERO) <= 0
                || dto.getHours().compareTo(new BigDecimal("24")) > 0) {
            throw new BizException(RCode.BAD_REQUEST, "工时必须在 (0, 24] 区间");
        }
        TaskHour h = new TaskHour();
        h.setItemId(dto.getItemId());
        h.setWorkDate(dto.getWorkDate());
        h.setHours(dto.getHours());
        h.setDescription(dto.getDescription());
        h.setEmpId(empId);
        mapper.insert(h);
        log.info("工时已登记: id={}, itemId={}, hours={}", h.getId(), h.getItemId(), h.getHours());
        return h.getId();
    }

    @Transactional
    public void delete(Long id, Long empId) {
        TaskHour h = mapper.selectById(id);
        if (h == null) throw new BizException(RCode.NOT_FOUND, "工时不存在");
        if (!h.getEmpId().equals(empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能删除自己的工时");
        }
        mapper.deleteById(id);
        log.info("工时已删除: id={}", id);
    }

    public List<TaskHourVO> listByItem(Long itemId) {
        List<TaskHour> list = mapper.findByItemId(itemId);
        return list.stream().map(this::toVO).toList();
    }

    /**
     * 按员工 + 日期范围查询 (供个人工时统计).
     */
    public List<TaskHourVO> listByEmpAndDateRange(Long empId, java.time.LocalDate from, java.time.LocalDate to) {
        List<TaskHour> list = mapper.findByEmpAndDateRange(empId, from, to);
        return list.stream().map(this::toVO).toList();
    }

    private TaskHourVO toVO(TaskHour h) {
        TaskHourVO vo = new TaskHourVO();
        vo.setId(h.getId());
        vo.setItemId(h.getItemId());
        vo.setWorkDate(h.getWorkDate());
        vo.setHours(h.getHours());
        vo.setDescription(h.getDescription());
        vo.setEmpId(h.getEmpId());
        vo.setCreateTime(h.getCreateTime());
        return vo;
    }
}
