package cn.oa.common.constant;

public final class LeaveType {

    public static final int ANNUAL = 1;
    public static final int PERSONAL = 2;
    public static final int SICK = 3;
    public static final int MARRIAGE = 4;
    public static final int MATERNITY = 5;
    public static final int BEREAVEMENT = 6;
    public static final int COMPENSATORY = 7;

    private static final String[] TEXT = {"", "年假", "事假", "病假", "婚假", "产假", "丧假", "调休"};

    private LeaveType() {
    }

    public static String text(int leaveType) {
        if (leaveType <= 0 || leaveType >= TEXT.length) {
            return "其他";
        }
        return TEXT[leaveType];
    }
}
