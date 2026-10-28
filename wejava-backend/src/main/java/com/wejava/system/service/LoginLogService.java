package com.wejava.system.service;

/**
 * 登录日志落库。异步执行，不阻塞登录响应。
 */
public interface LoginLogService {

    void record(String username, String ip, String userAgent, boolean success, String msg);
}
