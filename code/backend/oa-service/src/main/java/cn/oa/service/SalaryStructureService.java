package cn.oa.service;

import cn.oa.entity.OaSalaryStructure;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SalaryStructureService extends IService<OaSalaryStructure> {

    IPage<OaSalaryStructure> pageList(int pageNum, int pageSize, Long empId, String searchKey);
}
