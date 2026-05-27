package cn.oa.service.impl;

import cn.oa.entity.SysPost;
import cn.oa.mapper.SysPostMapper;
import cn.oa.service.PostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements PostService {

    @Override
    public IPage<SysPost> pageList(int pageNum, int pageSize, String postName, String postCode) {
        Page<SysPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(postName)) {
            wrapper.like(SysPost::getPostName, postName);
        }
        if (StringUtils.hasText(postCode)) {
            wrapper.like(SysPost::getPostCode, postCode);
        }
        wrapper.orderByAsc(SysPost::getPostSort);
        return this.page(page, wrapper);
    }
}
