package com.wejava.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 在线会话：会话内容 + 最后活跃时间。
 */
@Data
@AllArgsConstructor
public class OnlineSession {

    private String jti;

    private LoginSession session;

    private long lastActive;
}
