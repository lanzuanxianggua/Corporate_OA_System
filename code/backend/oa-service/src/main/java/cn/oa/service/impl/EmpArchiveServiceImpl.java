package cn.oa.service.impl;

import cn.oa.entity.OaEmpArchive;
import cn.oa.mapper.OaEmpArchiveMapper;
import cn.oa.service.EmpArchiveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class EmpArchiveServiceImpl extends ServiceImpl<OaEmpArchiveMapper, OaEmpArchive> implements EmpArchiveService {

    @Override
    public OaEmpArchive getByEmpId(Long empId) {
        return this.getOne(new LambdaQueryWrapper<OaEmpArchive>().eq(OaEmpArchive::getEmpId, empId));
    }

    @Override
    public OaEmpArchive getByEmpIdWithInfo(Long empId) {
        return baseMapper.getByEmpIdWithInfo(empId);
    }

    @Override
    public IPage<OaEmpArchive> pageWithEmpInfo(int pageNum, int pageSize, String searchKey) {
        Page<OaEmpArchive> page = new Page<>(pageNum, pageSize);
        return baseMapper.pageWithEmpInfo(page, searchKey);
    }
}
