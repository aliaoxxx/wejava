package com.wejava.system.controller;

import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.security.SecurityUtils;
import com.wejava.system.model.query.MessageQuery;
import com.wejava.system.model.vo.MessageVO;
import com.wejava.system.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内消息。属于个人数据，登录即可访问，不需要权限点。
 */
@RestController
@RequestMapping("/system/message")
@RequiredArgsConstructor
public class SysMessageController {

    private final MessageService messageService;

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(messageService.unreadCount(SecurityUtils.currentUserId()));
    }

    @GetMapping("/list")
    public R<PageResult<MessageVO>> list(MessageQuery query) {
        return R.ok(messageService.page(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/{messageId}")
    public R<MessageVO> detail(@PathVariable Long messageId) {
        return R.ok(messageService.getDetail(SecurityUtils.currentUserId(), messageId));
    }

    @PutMapping("/{messageId}/read")
    public R<Void> markRead(@PathVariable Long messageId) {
        messageService.markRead(SecurityUtils.currentUserId(), messageId);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        messageService.markAllRead(SecurityUtils.currentUserId());
        return R.ok();
    }
}
