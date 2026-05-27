package cn.oa.service.impl;

import cn.oa.entity.SysDictType;
import cn.oa.mapper.SysDictTypeMapper;
import cn.oa.service.DictTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements DictTypeService {

    @Override
    public IPage<SysDictType> pageList(int pageNum, int pageSize, String dictName, String dictType) {
        Page<SysDictType> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dictName)) {
            wrapper.like(SysDictType::getDictName, dictName);
        }
        if (StringUtils.hasText(dictType)) {
            wrapper.like(SysDictType::getDictType, dictType);
        }
        wrapper.orderByDesc(SysDictType::getCreateTime);
        return this.page(page, wrapper);
    }
}
