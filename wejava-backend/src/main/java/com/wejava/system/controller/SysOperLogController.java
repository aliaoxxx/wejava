package com.wejava.system.controller;

import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.system.model.query.SysOperLogQuery;
import com.wejava.system.model.vo.SysOperLogVO;
import com.wejava.system.service.OperLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor/operlog")
@RequiredArgsConstructor
public class SysOperLogController {

    private final OperLogService operLogService;

    @PreAuthorize("@ps.has('monitor:operlog:list')")
    @GetMapping("/list")
    public R<PageResult<SysOperLogVO>> list(SysOperLogQuery query) {
        return R.ok(operLogService.page(query));
    }

    @PreAuthorize("@ps.has('monitor:operlog:remove')")
    @DeleteMapping("/{operIds}")
    public R<Void> remove(@PathVariable List<Long> operIds) {
        operLogService.remove(operIds);
        return R.ok();
    }

    @PreAuthorize("@ps.has('monitor:operlog:remove')")
    @DeleteMapping("/clean")
    public R<Void> clean() {
        operLogService.clean();
        return R.ok();
    }
}
