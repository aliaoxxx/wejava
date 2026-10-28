package com.wejava.system.service.impl;

import com.wejava.common.core.PageResult;
import com.wejava.security.jwt.SessionStore;
import com.wejava.system.model.vo.OnlineUserVO;
import com.wejava.system.service.OnlineService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnlineServiceImpl implements OnlineService {

    private final SessionStore sessionStore;

    @Override
    public PageResult<OnlineUserVO> page(long pageNum, long pageSize) {
        long current = Math.max(pageNum, 1);
        long size = Math.min(Math.max(pageSize, 1), 100);
        List<OnlineUserVO> list = sessionStore.listOnline((current - 1) * size, size).stream()
                .map(item -> {
                    OnlineUserVO vo = new OnlineUserVO();
                    vo.setJti(item.getJti());
                    vo.setUserId(item.getSession().getUserId());
                    vo.setUsername(item.getSession().getUsername());
                    vo.setNickname(item.getSession().getNickname());
                    vo.setDeptId(item.getSession().getDeptId());
                    vo.setLastActiveTime(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(item.getLastActive()), ZoneId.systemDefault()));
                    return vo;
                })
                .toList();

        PageResult<OnlineUserVO> result = new PageResult<>();
        result.setTotal(sessionStore.countOnline());
        result.setList(list);
        return result;
    }

    @Override
    public void kick(String jti) {
        sessionStore.kickSession(jti);
    }
}
