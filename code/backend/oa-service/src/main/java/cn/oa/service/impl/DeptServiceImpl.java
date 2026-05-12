package cn.oa.service.impl;

import cn.oa.entity.SysDept;
import cn.oa.mapper.SysDeptMapper;
import cn.oa.service.DeptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements DeptService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<SysDept> getDeptTree() {
        // 查询所有部门
        List<SysDept> allDepts = this.list();
        // 递归组装树结构
        List<SysDept> tree = buildTree(allDepts, 0L);
        return tree;
    }

    @Override
    public SysDept getByParentId(Long parentId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getParentId, parentId);
        return this.getOne(wrapper);
    }

    /**
     * 递归构建部门树
     */
    private List<SysDept> buildTree(List<SysDept> allDepts, Long parentId) {
        return allDepts.stream()
                .filter(dept -> parentId.equals(dept.getParentId()))
                .collect(Collectors.toList());
    }

    /**
     * 保存部门后清除缓存
     */
    private void clearDeptCache() {
        redisTemplate.delete("dept:tree");
    }
}
