package cn.oa.hr.attendance.service;
import cn.oa.hr.attendance.entity.HrAttendanceGroup;
import cn.oa.hr.attendance.mapper.HrAttendanceGroupMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@RequiredArgsConstructor @Service
public class HrAttendanceGroupService {
    private final HrAttendanceGroupMapper mapper;
    @Transactional public Long create(HrAttendanceGroup g) { mapper.insert(g); return g.getId(); }
    @Transactional public void update(HrAttendanceGroup g) { mapper.updateById(g); }
    @Transactional public void delete(Long id) { mapper.deleteById(id); }
    public HrAttendanceGroup getById(Long id) { return mapper.selectById(id); }
    public List<HrAttendanceGroup> list(String status) {
        return mapper.selectList(new LambdaQueryWrapper<HrAttendanceGroup>().eq(status!=null, HrAttendanceGroup::getStatus, status));
    }
}