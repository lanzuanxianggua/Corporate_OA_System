package cn.oa.service;

import cn.oa.entity.SysPost;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PostService extends IService<SysPost> {

    IPage<SysPost> pageList(int pageNum, int pageSize, String postName, String postCode);
}
