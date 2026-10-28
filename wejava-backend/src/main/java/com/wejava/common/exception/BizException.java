package com.wejava.common.exception;

import lombok.Getter;

/**
 * 业务异常。由 GlobalExceptionHandler 统一转换为 R。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }
}
