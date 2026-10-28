package com.wejava.system.controller;

import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.system.model.vo.OnlineUserVO;
import com.wejava.system.service.OnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor/online")
@RequiredArgsConstructor
public class SysOnlineController {

    private final OnlineService onlineService;

    @PreAuthorize("@ps.has('monitor:online:list')")
    @GetMapping("/list")
    public R<PageResult<OnlineUserVO>> list(
            @RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(onlineService.page(pageNum, pageSize));
    }

    @PreAuthorize("@ps.has('monitor:online:forceLogout')")
    @DeleteMapping("/{jti}")
    public R<Void> kick(@PathVariable String jti) {
        onlineService.kick(jti);
        return R.ok();
    }
}
