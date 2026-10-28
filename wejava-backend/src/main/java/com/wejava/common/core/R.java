package com.wejava.common.core;

import com.wejava.common.exception.ErrorCode;
import com.wejava.common.web.TraceIdFilter;
import java.io.Serializable;
import lombok.Data;
import org.slf4j.MDC;

/**
 * 统一返回体。
 * <pre>
 * { "code": 200, "msg": "操作成功", "data": {...}, "traceId": "..." }
 * </pre>
 */
@Data
public class R<T> implements Serializable {

    private int code;
    private String msg;
    private T data;
    private String traceId;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return build(ErrorCode.SUCCESS.getCode(), "操作成功", data);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return build(code, msg, null);
    }

    private static <T> R<T> build(int code, String msg, T data) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        r.data = data;
        r.traceId = MDC.get(TraceIdFilter.TRACE_ID);
        return r;
    }
}
