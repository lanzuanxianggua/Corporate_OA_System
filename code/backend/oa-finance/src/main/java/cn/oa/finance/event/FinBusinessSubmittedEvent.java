package cn.oa.finance.event;

import lombok.Getter;

/**
 * 财务单据提交事件.
 *
 * <p>oa-finance 在 submit() 成功后发布此事件 (业务层 spring 事件, 非 oa-workflow 事件).
 * 当前阶段 oa-workflow 没有"提交流程"事件, 业务方如需"提交后通知审批人"等副作用,
 * 可在本模块内 {@code @EventListener} 监听, 或在 oa-message 内集成 (后续阶段).
 *
 * <p>本事件是 POJO, 不继承 {@code ApplicationEvent} (Spring 4.2+ 风格).
 */
@Getter
public class FinBusinessSubmittedEvent {

    /** 业务前缀: "EXP_" / "LOAN_". */
    private final String businessPrefix;

    /** 业务单据 ID. */
    private final Long businessId;

    /** 业务单据号 (展示用, 如 EXP1716000000000). */
    private final String applyNo;

    /** 提交人 empId. */
    private final Long submitterId;

    /** 流程实例 ID. */
    private final Long wfInstanceId;

    public FinBusinessSubmittedEvent(String businessPrefix, Long businessId, String applyNo,
                                    Long submitterId, Long wfInstanceId) {
        this.businessPrefix = businessPrefix;
        this.businessId = businessId;
        this.applyNo = applyNo;
        this.submitterId = submitterId;
        this.wfInstanceId = wfInstanceId;
    }
}
