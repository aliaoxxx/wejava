package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.common.sse.SseEventType;
import com.wejava.common.sse.SseHub;
import com.wejava.system.mapper.SysMessageMapper;
import com.wejava.system.mapper.SysMessageUserMapper;
import com.wejava.system.mapper.SysUserMapper;
import com.wejava.system.model.entity.SysMessage;
import com.wejava.system.model.entity.SysMessageUser;
import com.wejava.system.model.entity.SysUser;
import com.wejava.system.model.form.MessageSendForm;
import com.wejava.system.model.query.MessageQuery;
import com.wejava.system.model.vo.MessageVO;
import com.wejava.system.service.MessageService;
import com.wejava.system.service.MessageTargetResolver;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final SysMessageMapper messageMapper;
    private final SysMessageUserMapper messageUserMapper;
    private final SysUserMapper userMapper;
    private final SseHub sseHub;
    private final MessageTargetResolver targetResolver;

    @Override
    public long unreadCount(Long userId) {
        Long count = messageUserMapper.selectCount(Wrappers.<SysMessageUser>lambdaQuery()
                .eq(SysMessageUser::getUserId, userId)
                .eq(SysMessageUser::getReadFlag, 0));
        return count == null ? 0L : count;
    }

    @Override
    public PageResult<MessageVO> page(Long userId, MessageQuery query) {
        Page<MessageVO> page = messageUserMapper.selectInbox(query.toPage(), userId, query);
        return PageResult.of(page);
    }

    @Override
    public MessageVO getDetail(Long userId, Long messageId) {
        SysMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        SysMessageUser record = messageUserMapper.selectOne(Wrappers.<SysMessageUser>lambdaQuery()
                .eq(SysMessageUser::getUserId, userId)
                .eq(SysMessageUser::getMessageId, messageId));
        if (record == null) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return toVO(message, record.getReadFlag());
    }

    @Override
    public void markRead(Long userId, Long messageId) {
        SysMessageUser record = messageUserMapper.selectOne(Wrappers.<SysMessageUser>lambdaQuery()
                .eq(SysMessageUser::getUserId, userId)
                .eq(SysMessageUser::getMessageId, messageId));
        if (record == null) {
            return;
        }
        if (Integer.valueOf(0).equals(record.getReadFlag())) {
            record.setReadFlag(1);
            record.setReadTime(LocalDateTime.now());
            messageUserMapper.updateById(record);
        }
    }

    @Override
    public void markAllRead(Long userId) {
        messageUserMapper.update(
                null,
                Wrappers.<SysMessageUser>lambdaUpdate()
                        .eq(SysMessageUser::getUserId, userId)
                        .eq(SysMessageUser::getReadFlag, 0)
                        .set(SysMessageUser::getReadFlag, 1)
                        .set(SysMessageUser::getReadTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long send(MessageSendForm form) {
        int type = form.getMsgType() == null ? 2 : form.getMsgType();
        int level = form.getLevel() == null ? 0 : form.getLevel();
        var userIds = targetResolver.resolve(form.getTargetType(), form.getTargetIds());
        if (userIds.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "该范围内没有可接收的用户");
        }
        return doDeliver(form.getTitle(), form.getContent(), type, level, form.getLink(), null, userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long deliver(
            String title, String content, int msgType, int level, String link, Long sourceId, List<Long> userIds) {
        return doDeliver(title, content, msgType, level, link, sourceId, userIds);
    }

    /**
     * 实际的落库与推送。
     * <p>
     * 必须是 private 且不带 @Transactional：三个 public 入口各自已经是事务边界，
     * 在这里再加事务注解会因为「自调用不走代理」而失效（Spring AOP 的经典陷阱）。
     */
    private Long doDeliver(
            String title, String content, int msgType, int level, String link, Long sourceId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }
        SysMessage message = new SysMessage();
        message.setTitle(title);
        message.setContent(content);
        message.setMsgType(msgType);
        message.setLevel(level);
        message.setLink(link);
        message.setSourceId(sourceId);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);

        messageUserMapper.insertRecipients(message.getMessageId(), userIds);

        // 推送给在线用户；未在线的靠下次拉取补上
        sseHub.sendToUsers(
                userIds,
                SseEventType.MESSAGE,
                Map.of(
                        "messageId", String.valueOf(message.getMessageId()),
                        "title", message.getTitle(),
                        "msgType", message.getMsgType(),
                        "level", message.getLevel()));
        return message.getMessageId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long deliverToAll(String title, String content, int msgType, int level, String link, Long sourceId) {
        List<Long> userIds = userMapper
                .selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getStatus, 0)
                        .select(SysUser::getUserId))
                .stream()
                .map(SysUser::getUserId)
                .toList();
        return doDeliver(title, content, msgType, level, link, sourceId, userIds);
    }

    private MessageVO toVO(SysMessage message, Integer readFlag) {
        MessageVO vo = new MessageVO();
        vo.setMessageId(message.getMessageId());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setMsgType(message.getMsgType());
        vo.setLevel(message.getLevel());
        vo.setLink(message.getLink());
        vo.setReadFlag(readFlag == null ? 0 : readFlag);
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
