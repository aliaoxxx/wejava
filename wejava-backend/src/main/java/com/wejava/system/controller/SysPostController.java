package com.wejava.system.controller;

import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.system.model.form.SysPostForm;
import com.wejava.system.model.query.SysPostQuery;
import com.wejava.system.model.vo.PostOptionVO;
import com.wejava.system.model.vo.SysPostVO;
import com.wejava.system.service.SysPostService;
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
@RequestMapping("/system/post")
@RequiredArgsConstructor
public class SysPostController {

    private final SysPostService postService;

    @PreAuthorize("@ps.has('system:post:list')")
    @GetMapping("/list")
    public R<PageResult<SysPostVO>> list(SysPostQuery query) {
        return R.ok(postService.page(query));
    }

    @PreAuthorize("@ps.has('system:post:query')")
    @GetMapping("/{postId}")
    public R<SysPostVO> getById(@PathVariable Long postId) {
        return R.ok(postService.getById(postId));
    }

    @PreAuthorize("@ps.has('system:post:add')")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SysPostForm form) {
        return R.ok(postService.create(form));
    }

    @PreAuthorize("@ps.has('system:post:edit')")
    @PutMapping
    public R<Void> update(@RequestBody @Valid SysPostForm form) {
        postService.update(form);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:post:remove')")
    @DeleteMapping("/{postIds}")
    public R<Void> remove(@PathVariable List<Long> postIds) {
        postService.remove(postIds);
        return R.ok();
    }

    /** 岗位下拉：登录即可 */
    @GetMapping("/option-select")
    public R<List<PostOptionVO>> optionSelect() {
        return R.ok(postService.optionSelect());
    }
}
