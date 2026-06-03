package cn.oa.finance.mapper;

import cn.oa.finance.entity.FinLoanRepayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 借款还款记录Mapper
 *
 * @author oa-finance
 */
@Mapper
public interface FinLoanRepaymentMapper extends BaseMapper<FinLoanRepayment> {

}
