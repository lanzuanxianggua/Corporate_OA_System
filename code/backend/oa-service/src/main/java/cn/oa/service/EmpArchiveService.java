package cn.oa.service;

import cn.oa.entity.OaEmpArchive;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface EmpArchiveService extends IService<OaEmpArchive> {

    OaEmpArchive getByEmpId(Long empId);

    OaEmpArchive getByEmpIdWithInfo(Long empId);

    IPage<OaEmpArchive> pageWithEmpInfo(int pageNum, int pageSize, String searchKey);
}
