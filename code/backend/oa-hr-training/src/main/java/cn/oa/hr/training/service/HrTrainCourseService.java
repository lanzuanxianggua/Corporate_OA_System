package cn.oa.hr.training.service;
import cn.oa.hr.training.entity.HrTrainCourse;
import cn.oa.hr.training.mapper.HrTrainCourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@RequiredArgsConstructor @Service
public class HrTrainCourseService {
    private final HrTrainCourseMapper mapper;
    @Transactional public Long create(HrTrainCourse c) { mapper.insert(c); return c.getId(); }
    @Transactional public void update(HrTrainCourse c) { mapper.updateById(c); }
    @Transactional public void delete(Long id) { mapper.deleteById(id); }
    public HrTrainCourse getById(Long id) { return mapper.selectById(id); }
    public Page<HrTrainCourse> listPage(String status, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<HrTrainCourse>().eq(status!=null, HrTrainCourse::getStatus, status));
    }
}