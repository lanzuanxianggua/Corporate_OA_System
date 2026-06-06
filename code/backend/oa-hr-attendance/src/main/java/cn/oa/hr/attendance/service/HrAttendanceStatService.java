package cn.oa.hr.attendance.service;
import cn.oa.hr.attendance.dto.HrAttendanceQueryDTO;
import cn.oa.hr.attendance.entity.HrAttendanceStat;
import cn.oa.hr.attendance.mapper.HrAttendanceStatMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@RequiredArgsConstructor @Service
public class HrAttendanceStatService {
    private final HrAttendanceStatMapper mapper;
    public Page<HrAttendanceStat> listPage(HrAttendanceQueryDTO q) {
        return mapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()),
                new LambdaQueryWrapper<HrAttendanceStat>()
                .eq(q.getEmpId()!=null, HrAttendanceStat::getEmpId, q.getEmpId()));
    }
}