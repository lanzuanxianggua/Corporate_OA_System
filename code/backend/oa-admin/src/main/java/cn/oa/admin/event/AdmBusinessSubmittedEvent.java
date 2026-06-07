package cn.oa.admin.event;

import lombok.Getter;

/**
 * 行政事务提交事件.
 *
 * <p>oa-admin 在印章申请 / 资产领用 submit() 成功后发布此事件,
 * 供 callback / 通知中心监听.
 *
 * <p>设计参考 oa-finance 的 FinBusinessSubmittedEvent.
 */
@Getter
public class AdmBusinessSubmittedEvent {

    /** 业务前缀: "SEAL_" / "ASSET_". */
    private final String businessPrefix;

    /** 业务单据 ID. */
    private final Long businessId;

    /** 业务单据号 (展示用). */
    private final String applyNo;

    /** 提交人 empId. */
    private final Long submitterId;

    /** 流程实例 ID. */
    private final Long wfInstanceId;

    public AdmBusinessSubmittedEvent(String businessPrefix, Long businessId, String applyNo,
                                     Long submitterId, Long wfInstanceId) {
        this.businessPrefix = businessPrefix;
        this.businessId = businessId;
        this.applyNo = applyNo;
        this.submitterId = submitterId;
        this.wfInstanceId = wfInstanceId;
    }
}
