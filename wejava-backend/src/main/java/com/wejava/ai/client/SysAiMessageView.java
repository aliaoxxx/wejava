package com.wejava.ai.client;

/**
 * 发给模型的一条消息（只包含角色与内容）。
 */
public record SysAiMessageView(String role, String content) {}
