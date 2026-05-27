package cn.oa.service.impl;

import cn.oa.entity.SysDictData;
import cn.oa.mapper.SysDictDataMapper;
import cn.oa.service.DictDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements DictDataService {

    @Override
    public IPage<SysDictData> pageList(int pageNum, int pageSize, String dictType, String dictLabel) {
        Page<SysDictData> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dictType)) {
            wrapper.eq(SysDictData::getDictType, dictType);
        }
        if (StringUtils.hasText(dictLabel)) {
            wrapper.like(SysDictData::getDictLabel, dictLabel);
        }
        wrapper.orderByAsc(SysDictData::getDictSort);
        return this.page(page, wrapper);
    }

    @Override
    public List<SysDictData> getDictDataByType(String dictType) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType);
        wrapper.eq(SysDictData::getStatus, "0");
        wrapper.orderByAsc(SysDictData::getDictSort);
        return this.list(wrapper);
    }
}
