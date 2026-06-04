package cn.oa.workflow.service;

import cn.oa.platform.common.api.PageResult;
import cn.oa.workflow.entity.WfDefinition;
import cn.oa.workflow.mapper.WfDefinitionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程定义服务.
 */
@Service
public class WfDefinitionService {

    private final WfDefinitionMapper mapper;

    public WfDefinitionService(WfDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    public WfDefinition getById(Long id) { return mapper.selectById(id); }

    public List<WfDefinition> listActive() {
        return mapper.selectList(new LambdaQueryWrapper<WfDefinition>()
                .eq(WfDefinition::getStatus, "ACTIVE")
                .eq(WfDefinition::getDelFlag, 0)
                .orderByDesc(WfDefinition::getVersion));
    }

    public PageResult<WfDefinition> page(int pageNo, int pageSize) {
        Page<WfDefinition> page = new Page<>(pageNo, pageSize);
        Page<WfDefinition> result = mapper.selectPage(page,
                new LambdaQueryWrapper<WfDefinition>()
                        .eq(WfDefinition::getDelFlag, 0)
                        .orderByDesc(WfDefinition::getCreateTime));
        return PageResult.of(result.getRecords(), result.getTotal(),
                (int) result.getCurrent(), (int) result.getSize());
    }

    public Long create(WfDefinition entity) {
        if (entity.getDelFlag() == null) entity.setDelFlag("0");
        if (entity.getVersion() == null) entity.setVersion(1);
        if (entity.getStatus() == null) entity.setStatus("ACTIVE");
        mapper.insert(entity);
        return entity.getId();
    }

    public void update(WfDefinition entity) {
        mapper.updateById(entity);
    }

    public void delete(Long id) {
        WfDefinition def = new WfDefinition();
        def.setId(id);
        def.setDelFlag("1");
        mapper.updateById(def);
    }
}
