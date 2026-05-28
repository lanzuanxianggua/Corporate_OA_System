package cn.oa.aspect;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.utils.IpUtil;
import cn.oa.entity.OaOperationLog;
import cn.oa.mapper.OaOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OaOperationLogMapper operationLogMapper;

    @Around("@annotation(cn.oa.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        OaOperationLog logEntity = new OaOperationLog();
        Object result = null;
        try {
            result = point.proceed();
            logEntity.setStatus(1);
        } catch (Exception e) {
            logEntity.setStatus(0);
            throw e;
        } finally {
            try {
                long costTime = System.currentTimeMillis() - startTime;
                MethodSignature signature = (MethodSignature) point.getSignature();
                Method method = signature.getMethod();
                OperationLog annotation = method.getAnnotation(OperationLog.class);

                logEntity.setModule(annotation.module());
                logEntity.setOperation(annotation.operation());
                logEntity.setMethod(point.getTarget().getClass().getName() + "." + method.getName());
                logEntity.setCostTime(costTime);
                logEntity.setCreateTime(LocalDateTime.now());

                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    logEntity.setRequestUrl(request.getRequestURI());
                    logEntity.setIp(IpUtil.getClientIp(request));
                    Object empId = request.getAttribute("empId");
                    Object empName = request.getAttribute("empName");
                    if (empId != null) {
                        logEntity.setEmpId(Long.parseLong(empId.toString()));
                    }
                    if (empName != null) {
                        logEntity.setEmpName(empName.toString());
                    }
                }

                operationLogMapper.insert(logEntity);
            } catch (Exception e) {
                log.error("记录操作日志失败: {}", e.getMessage());
            }
        }
        return result;
    }
}
