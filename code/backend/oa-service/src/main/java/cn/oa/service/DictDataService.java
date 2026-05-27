package cn.oa.service;

import cn.oa.entity.SysDictData;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DictDataService extends IService<SysDictData> {

    IPage<SysDictData> pageList(int pageNum, int pageSize, String dictType, String dictLabel);

    List<SysDictData> getDictDataByType(String dictType);
}
