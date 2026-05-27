package cn.oa.service.impl;

import cn.oa.entity.OaContract;
import cn.oa.mapper.OaContractMapper;
import cn.oa.service.ContractService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ContractServiceImpl extends ServiceImpl<OaContractMapper, OaContract> implements ContractService {

    @Override
    public IPage<OaContract> pageList(int pageNum, int pageSize, String contractName, String contractType) {
        Page<OaContract> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaContract> wrapper = new LambdaQueryWrapper<>();
        if (contractName != null && !contractName.isEmpty()) {
            wrapper.like(OaContract::getContractName, contractName);
        }
        if (contractType != null && !contractType.isEmpty()) {
            wrapper.eq(OaContract::getContractType, contractType);
        }
        wrapper.orderByDesc(OaContract::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public List<OaContract> expiringList(int days) {
        LocalDate now = LocalDate.now();
        LocalDate deadline = now.plusDays(days);
        return this.list(new LambdaQueryWrapper<OaContract>()
                .ge(OaContract::getEndDate, now)
                .le(OaContract::getEndDate, deadline)
                .orderByAsc(OaContract::getEndDate));
    }
}
