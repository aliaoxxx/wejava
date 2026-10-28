package com.wejava.system.controller;

import com.wejava.common.core.R;
import com.wejava.system.model.form.SysDeptForm;
import com.wejava.system.model.query.SysDeptQuery;
import com.wejava.system.model.vo.SysDeptVO;
import com.wejava.system.service.SysDeptService;
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
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @PreAuthorize("@ps.has('system:dept:list')")
    @GetMapping("/list")
    public R<List<SysDeptVO>> list(SysDeptQuery query) {
        return R.ok(deptService.tree(query));
    }

    @PreAuthorize("@ps.has('system:dept:query')")
    @GetMapping("/{deptId}")
    public R<SysDeptVO> getById(@PathVariable Long deptId) {
        return R.ok(deptService.getById(deptId));
    }

    @PreAuthorize("@ps.has('system:dept:add')")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SysDeptForm form) {
        return R.ok(deptService.create(form));
    }

    @PreAuthorize("@ps.has('system:dept:edit')")
    @PutMapping
    public R<Void> update(@RequestBody @Valid SysDeptForm form) {
        deptService.update(form);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:dept:remove')")
    @DeleteMapping("/{deptId}")
    public R<Void> remove(@PathVariable Long deptId) {
        deptService.remove(deptId);
        return R.ok();
    }

    /** 部门树选择器：登录即可 */
    @GetMapping("/tree-select")
    public R<List<SysDeptVO>> treeSelect() {
        return R.ok(deptService.treeSelect());
    }
}
