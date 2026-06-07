package cn.oa.document.event;

import lombok.Getter;

/**
 * 文档业务提交事件 (oa-document 内部事件).
 *
 * <p>oa-document 在发文/签报 submit() 成功后发布此事件, 通知中心/工作流回调可监听.
 * 设计参考 oa-finance 的 FinBusinessSubmittedEvent.
 */
@Getter
public class DocBusinessSubmittedEvent {

    /** 业务前缀: "DISPATCH_" / "SIGN_REPORT_". */
    private final String businessPrefix;

    /** 业务单据 ID. */
    private final Long businessId;

    /** 业务单据号 (展示用, 如 DOC1716000000000). */
    private final String docNo;

    /** 提交人 empId. */
    private final Long submitterId;

    /** 流程实例 ID. */
    private final Long wfInstanceId;

    public DocBusinessSubmittedEvent(String businessPrefix, Long businessId, String docNo,
                                     Long submitterId, Long wfInstanceId) {
        this.businessPrefix = businessPrefix;
        this.businessId = businessId;
        this.docNo = docNo;
        this.submitterId = submitterId;
        this.wfInstanceId = wfInstanceId;
    }
}
