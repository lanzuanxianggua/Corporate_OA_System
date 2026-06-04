package cn.oa.platform.common.exception;

import cn.oa.platform.common.api.R;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public R<?> handleBiz(BizException e, HttpServletRequest request) {
        log.warn("BizException: code={}, message={}, uri={}, user={}",
                e.getCode(), e.getMessage(), request.getRequestURI(), currentUser());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public R<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", msg);
        return R.fail(RCode.VALIDATION_FAILED.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public R<?> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.joining("; "));
        return R.fail(RCode.VALIDATION_FAILED.getCode(), msg);
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return R.fail(RCode.METHOD_NOT_ALLOWED, e.getMessage());
    }

    @ExceptionHandler({NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> handleNoHandler(NoHandlerFoundException e) {
        return R.fail(RCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleBadRequest(HttpMessageNotReadableException e) {
        return R.fail(RCode.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(RCode.BAD_REQUEST, "缺少参数: " + e.getParameterName());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleUnknown(Exception e, HttpServletRequest request) {
        log.error("Unknown exception: uri={}, user={}", request.getRequestURI(), currentUser(), e);
        return R.fail(RCode.INTERNAL_ERROR);
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    private String formatConstraintViolation(ConstraintViolation<?> v) {
        String path = v.getPropertyPath().toString();
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return field + ": " + v.getMessage();
    }

    private String currentUser() {
        Long empId = UserContext.getCurrentEmpId();
        return empId == null ? "anonymous" : String.valueOf(empId);
    }
}
