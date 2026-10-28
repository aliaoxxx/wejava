package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.query.MessageQuery;
import com.wejava.system.model.vo.MessageVO;
import java.util.List;

/**
 * 站内消息。所有接口只作用于当前登录用户的收件箱。
 */
public interface MessageService {

    long unreadCount(Long userId);

    PageResult<MessageVO> page(Long userId, MessageQuery query);

    MessageVO getDetail(Long userId, Long messageId);

    void markRead(Long userId, Long messageId);

    void markAllRead(Long userId);

    /** 投递给指定用户 */
    Long deliver(String title, String content, int msgType, int level, String link, Long sourceId, List<Long> userIds);

    /** 投递给全部启用状态的用户（公告发布用） */
    Long deliverToAll(String title, String content, int msgType, int level, String link, Long sourceId);
}
