package com.wejava.common.exception;

import lombok.Getter;

/**
 * 触发限流。由 GlobalExceptionHandler 转成 HTTP 429。
 */
@Getter
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
