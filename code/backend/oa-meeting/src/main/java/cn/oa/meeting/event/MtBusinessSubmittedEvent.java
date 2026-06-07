package cn.oa.meeting.event;

import lombok.Getter;

/**
 * 会议业务提交事件.
 *
 * <p>oa-meeting 在预约 submit() 成功后发布此事件, 供 callback / 通知中心监听.
 *
 * <p>设计参考 oa-finance FinBusinessSubmittedEvent.
 */
@Getter
public class MtBusinessSubmittedEvent {

    /** 业务前缀: "BOOKING_". */
    private final String businessPrefix;

    /** 业务单据 ID. */
    private final Long businessId;

    /** 业务单据号 (展示用). */
    private final String bookNo;

    /** 提交人 empId. */
    private final Long submitterId;

    /** 流程实例 ID. */
    private final Long wfInstanceId;

    public MtBusinessSubmittedEvent(String businessPrefix, Long businessId, String bookNo,
                                    Long submitterId, Long wfInstanceId) {
        this.businessPrefix = businessPrefix;
        this.businessId = businessId;
        this.bookNo = bookNo;
        this.submitterId = submitterId;
        this.wfInstanceId = wfInstanceId;
    }
}
