package com.wejava.common.sse;

/**
 * SSE 事件类型。前端按 event 名分发处理。
 */
public final class SseEventType {

    /** 握手成功 */
    public static final String CONNECTED = "connected";

    /** 业务消息（新公告 / 系统消息） */
    public static final String MESSAGE = "message";

    /** 未读数变化 */
    public static final String UNREAD = "unread";

    /** 服务端主动报错 */
    public static final String ERROR = "error";

    private SseEventType() {}
}
