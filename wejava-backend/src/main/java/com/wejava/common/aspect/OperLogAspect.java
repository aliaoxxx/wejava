package com.wejava.common.aspect;

import com.wejava.common.annotation.OperLog;
import com.wejava.common.web.IpUtils;
import com.wejava.security.SecurityUtils;
import com.wejava.system.model.entity.SysOperLog;
import com.wejava.system.service.OperLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

/**
 * 操作日志切面：包住标注了 @OperLog 的方法，成功后异步落库。
 * 日志失败绝不影响业务，所有异常在内部吞掉。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private static final int MAX_LENGTH = 2000;

    private final OperLogService operLogService;
    private final JsonMapper jsonMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                operLogService.record(build(joinPoint, operLog, result, error, System.currentTimeMillis() - start));
            } catch (Exception e) {
                log.warn("操作日志记录失败: {}", e.getMessage());
            }
        }
    }

    private SysOperLog build(
            ProceedingJoinPoint joinPoint, OperLog annotation, Object result, Throwable error, long costTime) {
        SysOperLog entity = new SysOperLog();
        entity.setTitle(annotation.title());
        entity.setBusinessType(annotation.businessType().getCode());
        entity.setOperatorType(1);
        entity.setMethod(joinPoint.getSignature().getDeclaringTypeName() + "."
                + joinPoint.getSignature().getName());
        entity.setCostTime(costTime);
        entity.setOperTime(LocalDateTime.now());
        entity.setStatus(error == null ? 0 : 1);
        entity.setOperName(SecurityUtils.currentUsername());

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            entity.setRequestMethod(request.getMethod());
            entity.setOperUrl(request.getRequestURI());
            entity.setOperIp(IpUtils.getIp(request));
        }

        if (annotation.saveParam()) {
            entity.setOperParam(truncate(toJson(collectArgs(joinPoint.getArgs()))));
        }
        if (error == null) {
            entity.setJsonResult(truncate(toJson(result)));
        } else {
            entity.setErrorMsg(truncate(error.getMessage()));
        }
        return entity;
    }

    private List<Object> collectArgs(Object[] args) {
        List<Object> list = new ArrayList<>();
        if (args == null) {
            return list;
        }
        for (Object arg : args) {
            if (arg == null
                    || arg instanceof HttpServletRequest
                    || arg instanceof jakarta.servlet.http.HttpServletResponse
                    || arg instanceof MultipartFile
                    || arg instanceof BindingResult) {
                continue;
            }
            list.add(arg);
        }
        return list;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String json = jsonMapper.writeValueAsString(value);
            // 敏感字段脱敏
            return json.replaceAll("(\"(?:password|oldPassword|newPassword)\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value;
    }
}
