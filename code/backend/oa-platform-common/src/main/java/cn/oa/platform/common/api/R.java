package cn.oa.platform.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Objects;

/**
 * 统一响应 (v2).
 *
 * <p>v2 设计：docs/v2/03-api-spec.md §2
 */
@Schema(description = "统一响应")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "错误码, 0=成功")
    private Integer code;

    @Schema(description = "错误信息")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "服务端时间戳(毫秒)")
    private Long timestamp;

    public R() {
    }

    public R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        return new R<>(0, "ok", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(0, "ok", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(0, message, data);
    }

    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(ResultCode rc) {
        return new R<>(rc.getCode(), rc.getMessage(), null);
    }

    public static <T> R<T> fail(ResultCode rc, String message) {
        return new R<>(rc.getCode(), message, null);
    }

    public boolean isSuccess() {
        return this.code != null && this.code == 0;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        R<?> r = (R<?>) o;
        return Objects.equals(code, r.code) && Objects.equals(timestamp, r.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, timestamp);
    }

    @Override
    public String toString() {
        return "R{code=" + code + ", message=" + message + ", data=" + data + ", traceId=" + traceId + "}";
    }
}
