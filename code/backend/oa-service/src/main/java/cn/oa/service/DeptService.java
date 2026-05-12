package cn.oa.service;

import cn.oa.entity.SysDept;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DeptService extends IService<SysDept> {

    /**
     * 获取部门树
     */
    List<SysDept> getDeptTree();

    /**
     * 根据父ID获取子部门
     */
    SysDept getByParentId(Long parentId);
}
