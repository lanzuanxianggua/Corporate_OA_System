package cn.oa.service;

import cn.oa.entity.OaExpense;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ExpenseService extends IService<OaExpense> {

    void submit(OaExpense expense);

    void approve(Long applyId, Long approverId, Integer status, String remark);

    IPage<OaExpense> pageList(int pageNum, int pageSize, Long empId, Integer status);
}
