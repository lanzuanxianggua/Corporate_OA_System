package cn.oa.common.utils;

public final class PageParamUtil {

    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private PageParamUtil() {
    }

    public static int pageNum(int pageNum) {
        return Math.max(pageNum, DEFAULT_PAGE_NUM);
    }

    public static int pageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }
}
