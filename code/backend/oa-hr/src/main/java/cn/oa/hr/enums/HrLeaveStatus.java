package cn.oa.hr.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HR请假状态枚举
 * 对应 hr_leave_apply.status 字段
 *
 * @author oa-hr
 */
@Getter
@AllArgsConstructor
public enum HrLeaveStatus {

    DRAFT("DRAFT", "草稿"),
    RUNNING("RUNNING", "审批中"),
    PASSED("PASSED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    REVOKED("REVOKED", "已撤回");

    private final String code;
    private final String name;

    /**
     * 根据code获取枚举
     */
    public static HrLeaveStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (HrLeaveStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否可以撤回
     */
    public boolean canRevoke() {
        return this == RUNNING;
    }

    /**
     * 判断是否可以重提
     */
    public boolean canResubmit() {
        return this == REJECTED;
    }

    /**
     * 是否终态
     */
    public boolean isFinal() {
        return this == PASSED || this == REJECTED || this == REVOKED;
    }
}
