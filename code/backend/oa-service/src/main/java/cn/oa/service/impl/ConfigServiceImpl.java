package cn.oa.service.impl;

import cn.oa.entity.SysConfig;
import cn.oa.mapper.SysConfigMapper;
import cn.oa.service.ConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ConfigService {

    @Override
    public IPage<SysConfig> pageList(int pageNum, int pageSize, String configName, String configKey) {
        Page<SysConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(configName)) {
            wrapper.like(SysConfig::getConfigName, configName);
        }
        if (StringUtils.hasText(configKey)) {
            wrapper.like(SysConfig::getConfigKey, configKey);
        }
        wrapper.orderByDesc(SysConfig::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public SysConfig getByKey(String configKey) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        return this.getOne(wrapper);
    }
}
