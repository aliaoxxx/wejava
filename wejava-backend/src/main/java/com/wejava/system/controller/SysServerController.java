package com.wejava.system.controller;

import com.wejava.common.core.R;
import com.wejava.system.model.vo.ServerInfoVO;
import com.wejava.system.service.ServerMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor/server")
@RequiredArgsConstructor
public class SysServerController {

    private final ServerMonitorService serverMonitorService;

    @PreAuthorize("@ps.has('monitor:server:list')")
    @GetMapping
    public R<ServerInfoVO> info() {
        return R.ok(serverMonitorService.info());
    }
}
