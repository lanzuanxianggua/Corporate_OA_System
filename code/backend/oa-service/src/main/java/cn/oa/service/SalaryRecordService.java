package cn.oa.service;

import cn.oa.entity.OaSalaryRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SalaryRecordService extends IService<OaSalaryRecord> {

    IPage<OaSalaryRecord> pageList(int pageNum, int pageSize, Long empId, String salaryMonth, String searchKey);

    OaSalaryRecord myLatestRecord(Long empId);

    void generateMonthlyRecord(Long empId, String month);
}
