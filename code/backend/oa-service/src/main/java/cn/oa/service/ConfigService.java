package cn.oa.service;

import cn.oa.entity.SysConfig;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ConfigService extends IService<SysConfig> {

    IPage<SysConfig> pageList(int pageNum, int pageSize, String configName, String configKey);

    SysConfig getByKey(String configKey);
}
