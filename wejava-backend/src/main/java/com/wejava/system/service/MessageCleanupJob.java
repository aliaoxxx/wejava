package com.wejava.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wejava.system.mapper.SysConfigMapper;
import com.wejava.system.mapper.SysMessageMapper;
import com.wejava.system.mapper.SysMessageUserMapper;
import com.wejava.system.model.entity.SysConfig;
import com.wejava.system.model.entity.SysMessageUser;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息保留策略：每天凌晨清理「已读且超过保留天数」的收件箱记录，
 * 再删掉没有接收人的消息本体。
 * <p>
 * 只清理已读消息——未读消息无论多旧都保留，避免用户永远看不到本该看到的东西。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCleanupJob {

    private static final String CONFIG_KEY = "sys.message.retentionDays";
    private static final int DEFAULT_RETENTION_DAYS = 30;

    private final SysMessageUserMapper messageUserMapper;
    private final SysMessageMapper messageMapper;
    private final SysConfigMapper configMapper;

    @Scheduled(cron = "0 30 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void cleanup() {
        int days = retentionDays();
        if (days <= 0) {
            log.info("消息保留天数配置为 {}，跳过清理", days);
            return;
        }
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        int removed = messageUserMapper.delete(Wrappers.<SysMessageUser>lambdaQuery()
                .eq(SysMessageUser::getReadFlag, 1)
                .lt(SysMessageUser::getReadTime, before));
        int orphans = messageMapper.deleteOrphanMessages(before);
        log.info("消息清理完成：已读记录 {} 条，空消息 {} 条（保留 {} 天）", removed, orphans, days);
    }

    private int retentionDays() {
        SysConfig config =
                configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, CONFIG_KEY));
        if (config == null
                || config.getConfigValue() == null
                || config.getConfigValue().isBlank()) {
            return DEFAULT_RETENTION_DAYS;
        }
        try {
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (NumberFormatException e) {
            return DEFAULT_RETENTION_DAYS;
        }
    }
}
