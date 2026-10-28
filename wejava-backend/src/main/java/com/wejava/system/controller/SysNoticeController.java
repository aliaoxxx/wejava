package com.wejava.system.controller;

import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.system.model.form.SysNoticeForm;
import com.wejava.system.model.query.SysNoticeQuery;
import com.wejava.system.model.vo.SysNoticeVO;
import com.wejava.system.service.SysNoticeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/notice")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeService noticeService;

    @PreAuthorize("@ps.has('system:notice:list')")
    @GetMapping("/list")
    public R<PageResult<SysNoticeVO>> list(SysNoticeQuery query) {
        return R.ok(noticeService.page(query));
    }

    @PreAuthorize("@ps.has('system:notice:query')")
    @GetMapping("/{noticeId}")
    public R<SysNoticeVO> getById(@PathVariable Long noticeId) {
        return R.ok(noticeService.getById(noticeId));
    }

    @PreAuthorize("@ps.has('system:notice:add')")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SysNoticeForm form) {
        return R.ok(noticeService.create(form));
    }

    @PreAuthorize("@ps.has('system:notice:edit')")
    @PutMapping
    public R<Void> update(@RequestBody @Valid SysNoticeForm form) {
        noticeService.update(form);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:notice:remove')")
    @DeleteMapping("/{noticeIds}")
    public R<Void> remove(@PathVariable List<Long> noticeIds) {
        noticeService.remove(noticeIds);
        return R.ok();
    }
}
