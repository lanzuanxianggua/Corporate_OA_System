package cn.oa.service;

import cn.oa.entity.OaContract;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ContractService extends IService<OaContract> {

    IPage<OaContract> pageList(int pageNum, int pageSize, String contractName, String contractType, String contractNo);

    List<OaContract> expiringList(int days);
}
