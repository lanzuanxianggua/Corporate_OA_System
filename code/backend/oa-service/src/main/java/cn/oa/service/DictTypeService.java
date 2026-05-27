package cn.oa.service;

import cn.oa.entity.SysDictType;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface DictTypeService extends IService<SysDictType> {

    IPage<SysDictType> pageList(int pageNum, int pageSize, String dictName, String dictType);
}
