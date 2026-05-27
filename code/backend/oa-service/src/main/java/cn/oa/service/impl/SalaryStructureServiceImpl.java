package cn.oa.service.impl;

import cn.oa.entity.OaSalaryStructure;
import cn.oa.mapper.OaSalaryStructureMapper;
import cn.oa.service.SalaryStructureService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SalaryStructureServiceImpl extends ServiceImpl<OaSalaryStructureMapper, OaSalaryStructure> implements SalaryStructureService {

    @Override
    public IPage<OaSalaryStructure> pageList(int pageNum, int pageSize, Long empId, String searchKey) {
        Page<OaSalaryStructure> page = new Page<>(pageNum, pageSize);
        return baseMapper.pageWithEmpInfo(page, empId, searchKey);
    }
}
