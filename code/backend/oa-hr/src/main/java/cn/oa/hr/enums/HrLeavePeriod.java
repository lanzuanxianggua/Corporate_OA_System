package cn.oa.hr.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HR请假时段枚举
 * 对应 hr_leave_apply.leave_period 字段
 *
 * @author oa-hr
 */
@Getter
@AllArgsConstructor
public enum HrLeavePeriod {

    FULL("FULL", "全天"),
    AM("AM", "上午"),
    PM("PM", "下午");

    private final String code;
    private final String name;

    /**
     * 根据code获取枚举
     */
    public static HrLeavePeriod fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (HrLeavePeriod period : values()) {
            if (period.getCode().equals(code)) {
                return period;
            }
        }
        return null;
    }

    /**
     * 获取天数系数
     */
    public double getDaysMultiplier() {
        return this == FULL ? 1.0 : 0.5;
    }
}
