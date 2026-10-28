package com.wejava.system.controller;

import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.security.jwt.LoginAttemptStore;
import com.wejava.system.model.query.SysLoginLogQuery;
import com.wejava.system.model.vo.SysLoginLogVO;
import com.wejava.system.service.LoginLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor/loginlog")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final LoginLogService loginLogService;
    private final LoginAttemptStore loginAttemptStore;

    @PreAuthorize("@ps.has('monitor:loginlog:list')")
    @GetMapping("/list")
    public R<PageResult<SysLoginLogVO>> list(SysLoginLogQuery query) {
        return R.ok(loginLogService.page(query));
    }

    @PreAuthorize("@ps.has('monitor:loginlog:remove')")
    @DeleteMapping("/{infoIds}")
    public R<Void> remove(@PathVariable List<Long> infoIds) {
        loginLogService.remove(infoIds);
        return R.ok();
    }

    @PreAuthorize("@ps.has('monitor:loginlog:remove')")
    @DeleteMapping("/clean")
    public R<Void> clean() {
        loginLogService.clean();
        return R.ok();
    }

    /** 账户解锁：清除失败计数与锁定标记 */
    @PreAuthorize("@ps.has('monitor:loginlog:unlock')")
    @PutMapping("/unlock/{username}")
    public R<Void> unlock(@PathVariable String username) {
        loginAttemptStore.clearFail(username);
        return R.ok();
    }
}
