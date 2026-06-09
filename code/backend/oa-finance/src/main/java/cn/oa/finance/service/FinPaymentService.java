package cn.oa.finance.service;

import cn.oa.finance.entity.FinPayment;
import cn.oa.finance.mapper.FinPaymentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class FinPaymentService {
    private static final AtomicInteger SEQ = new AtomicInteger();
    private final FinPaymentMapper mapper;

    @Transactional
    public Long create(FinPayment payment) {
        if (payment.getPaymentNo() == null || payment.getPaymentNo().isBlank()) {
            payment.setPaymentNo("PAY" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + String.format("%04d", SEQ.incrementAndGet() % 10000));
        }
        if (payment.getStatus() == null) payment.setStatus("DRAFT");
        mapper.insert(payment);
        return payment.getId();
    }

    @Transactional public void update(FinPayment payment) { mapper.updateById(payment); }
    @Transactional public void delete(Long id) { mapper.deleteById(id); }
    public FinPayment getById(Long id) { return mapper.selectById(id); }

    @Transactional
    public void submit(Long id) {
        FinPayment payment = mapper.selectById(id);
        payment.setStatus("PENDING");
        mapper.updateById(payment);
    }

    @Transactional
    public void markPaid(Long id, String payMethod) {
        FinPayment payment = mapper.selectById(id);
        payment.setStatus("PAID");
        payment.setPayMethod(payMethod);
        payment.setPaidTime(LocalDateTime.now());
        mapper.updateById(payment);
    }

    /**
     * V1010: Workflow callback hook. Maps integer status to the payment lifecycle.
     * <ul>
     *   <li>1 → PAID (approved, finance has disbursed)</li>
     *   <li>2 → DRAFT (rejected — revert so the requester can edit and resubmit)</li>
     *   <li>3 → DRAFT (withdrawn — same reasoning as rejected)</li>
     * </ul>
     * <p>Note: in real life "approved" should usually set status to APPROVED rather
     * than PAID, but the current schema only has DRAFT/PENDING/PAID, so we collapse
     * the intermediate state. A future migration can add an APPROVED state.
     */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        FinPayment payment = mapper.selectById(id);
        if (payment == null) return;
        if (status == null) return;
        switch (status) {
            case 1: payment.setStatus("PAID");   break;
            case 2: payment.setStatus("DRAFT");  break;
            case 3: payment.setStatus("DRAFT");  break;
            default: return;
        }
        mapper.updateById(payment);
    }

    public Page<FinPayment> listPage(Long contractId, String status, int pn, int ps) {
        return mapper.selectPage(new Page<>(pn, ps), new LambdaQueryWrapper<FinPayment>()
                .eq(contractId != null, FinPayment::getContractId, contractId)
                .eq(status != null && !status.isBlank(), FinPayment::getStatus, status)
                .orderByDesc(FinPayment::getCreateTime));
    }
}
