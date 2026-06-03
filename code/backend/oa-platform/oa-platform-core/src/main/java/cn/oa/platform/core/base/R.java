package cn.oa.platform.core.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        return new R<>(0, "操作成功", data);
    }

    public static <T> R<T> ok() {
        return new R<>(0, "操作成功", null);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(-1, message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> unauthorized() {
        return new R<>(401, "未授权", null);
    }
}