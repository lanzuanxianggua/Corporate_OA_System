package cn.oa.platform.core.constant;

/**
 * 常量定义
 *
 * @author oa-platform
 */
public final class Constants {

    private Constants() {
        // 私有构造函数，防止实例化
    }

    // ==================== 字符串常量 ====================

    /**
     * UTF-8 编码
     */
    public static final String UTF8 = "UTF-8";

    /**
     * JSON 内容类型
     */
    public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    /**
     * 成功消息
     */
    public static final String SUCCESS_MSG = "操作成功";

    /**
     * 失败消息
     */
    public static final String FAIL_MSG = "操作失败";

    // ==================== 数字常量 ====================

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    // ==================== Token 相关常量 ====================

    /**
     * Token 请求头名称
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 默认 Token 过期时间（小时）
     */
    public static final int DEFAULT_TOKEN_EXPIRE_HOURS = 2;

    // ==================== Redis Key 前缀 ====================

    /**
     * Token 缓存前缀
     */
    public static final String REDIS_TOKEN_PREFIX = "token:";

    /**
     * 用户缓存前缀
     */
    public static final String REDIS_USER_PREFIX = "user:";

    /**
     * 在线用户前缀
     */
    public static final String REDIS_ONLINE_PREFIX = "online:user:";

    /**
     * 验证码前缀
     */
    public static final String REDIS_CAPTCHA_PREFIX = "captcha:";

    /**
     * 未读消息数前缀
     */
    public static final String REDIS_MSG_UNREAD_PREFIX = "msg:unread:";

    // ==================== 逻辑删除标记 ====================

    /**
     * 未删除
     */
    public static final int DEL_FLAG_NORMAL = 0;

    /**
     * 已删除
     */
    public static final int DEL_FLAG_DELETED = 1;

    // ==================== 角色常量 ====================

    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "ADMIN";

    /**
     * 普通用户角色
     */
    public static final String ROLE_USER = "USER";

    // ==================== 日期格式 ====================

    /**
     * 默认日期格式
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 默认时间格式
     */
    public static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * 默认日期时间格式
     */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 时间戳格式
     */
    public static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmssSSS";
}
