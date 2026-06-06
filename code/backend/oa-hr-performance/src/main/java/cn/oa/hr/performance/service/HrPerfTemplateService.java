package cn.oa.hr.performance.service;
import cn.oa.hr.performance.entity.HrPerfTemplate;
import cn.oa.hr.performance.mapper.HrPerfTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@RequiredArgsConstructor @Service
public class HrPerfTemplateService {
    private final HrPerfTemplateMapper mapper;
    @Transactional public Long create(HrPerfTemplate t) { mapper.insert(t); return t.getId(); }
    @Transactional public void update(HrPerfTemplate t) { mapper.updateById(t); }
    @Transactional public void delete(Long id) { mapper.deleteById(id); }
    public HrPerfTemplate getById(Long id) { return mapper.selectById(id); }
    public List<HrPerfTemplate> list(String status) {
        return mapper.selectList(new LambdaQueryWrapper<HrPerfTemplate>().eq(status!=null, HrPerfTemplate::getStatus, status));
    }
}