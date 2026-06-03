package cn.oa.platform.core.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体
 *
 * @param <T> 数据类型
 * @author oa-platform
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功状态码
     */
    public static final int SUCCESS_CODE = 0;

    /**
     * 失败状态码
     */
    public static final int FAIL_CODE = -1;

    /**
     * 未授权状态码
     */
    public static final int UNAUTHORIZED_CODE = 401;

    /**
     * 禁止访问状态码
     */
    public static final int FORBIDDEN_CODE = 403;

    /**
     * 资源未找到状态码
     */
    public static final int NOT_FOUND_CODE = 404;

    /**
     * 服务器错误状态码
     */
    public static final int SERVER_ERROR_CODE = 500;

    /**
     * 状态码
     */
    private int code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 成功响应
     */
    public static <T> R<T> ok() {
        return new R<>(SUCCESS_CODE, "操作成功", null);
    }

    /**
     * 成功响应（带消息）
     */
    public static <T> R<T> ok(String message) {
        return new R<>(SUCCESS_CODE, message, null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS_CODE, "操作成功", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> R<T> ok(String message, T data) {
        return new R<>(SUCCESS_CODE, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> R<T> fail() {
        return new R<>(FAIL_CODE, "操作失败", null);
    }

    /**
     * 失败响应（带消息）
     */
    public static <T> R<T> fail(String message) {
        return new R<>(FAIL_CODE, message, null);
    }

    /**
     * 失败响应（带状态码和消息）
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    /**
     * 未授权响应
     */
    public static <T> R<T> unauthorized() {
        return new R<>(UNAUTHORIZED_CODE, "未授权，请先登录", null);
    }

    /**
     * 未授权响应（带消息）
     */
    public static <T> R<T> unauthorized(String message) {
        return new R<>(UNAUTHORIZED_CODE, message, null);
    }

    /**
     * 禁止访问响应
     */
    public static <T> R<T> forbidden() {
        return new R<>(FORBIDDEN_CODE, "禁止访问", null);
    }

    /**
     * 禁止访问响应（带消息）
     */
    public static <T> R<T> forbidden(String message) {
        return new R<>(FORBIDDEN_CODE, message, null);
    }

    /**
     * 资源未找到响应
     */
    public static <T> R<T> notFound() {
        return new R<>(NOT_FOUND_CODE, "资源未找到", null);
    }

    /**
     * 资源未找到响应（带消息）
     */
    public static <T> R<T> notFound(String message) {
        return new R<>(NOT_FOUND_CODE, message, null);
    }

    /**
     * 服务器错误响应
     */
    public static <T> R<T> serverError() {
        return new R<>(SERVER_ERROR_CODE, "服务器内部错误", null);
    }

    /**
     * 服务器错误响应（带消息）
     */
    public static <T> R<T> serverError(String message) {
        return new R<>(SERVER_ERROR_CODE, message, null);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return SUCCESS_CODE == this.code;
    }

    /**
     * 判断是否失败
     */
    public boolean isFail() {
        return !isSuccess();
    }
}
