package com.wejava.system.controller;

import com.wejava.common.core.R;
import com.wejava.common.sse.SseHub;
import com.wejava.common.sse.SseTicketService;
import com.wejava.security.SecurityUtils;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseTicketService ticketService;
    private final SseHub sseHub;

    /** 换取一次性票据（30 秒、用一次即失效） */
    @PostMapping("/auth/sse-ticket")
    public R<Map<String, Object>> ticket() {
        String ticket = ticketService.issue(SecurityUtils.currentUserId());
        return R.ok(Map.of("ticket", ticket, "expiresIn", 30));
    }

    /**
     * 通知通道。EventSource 无法携带 Authorization 头，因此用票据换连接。
     */
    @GetMapping(value = "/stream/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter notifications(@RequestParam String ticket) {
        Long userId = ticketService.consume(ticket);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "连接票据无效或已过期");
        }
        return sseHub.connect(userId);
    }
}
