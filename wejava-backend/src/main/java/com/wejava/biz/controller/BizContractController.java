package com.wejava.biz.controller;

import com.wejava.biz.model.form.BizContractForm;
import com.wejava.biz.model.query.BizContractQuery;
import com.wejava.biz.model.vo.BizContractVO;
import com.wejava.biz.service.BizContractService;
import com.wejava.common.annotation.BusinessType;
import com.wejava.common.annotation.OperLog;
import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
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

/** 合同管理 */
@RestController
@RequestMapping("/biz/contract")
@RequiredArgsConstructor
public class BizContractController {

    private final BizContractService contractService;

    @PreAuthorize("@ps.has('biz:contract:list')")
    @GetMapping("/list")
    public R<PageResult<BizContractVO>> list(BizContractQuery query) {
        return R.ok(contractService.page(query));
    }

    @PreAuthorize("@ps.has('biz:contract:query')")
    @GetMapping("/{contractId}")
    public R<BizContractVO> getById(@PathVariable Long contractId) {
        return R.ok(contractService.getById(contractId));
    }

    @PreAuthorize("@ps.has('biz:contract:add')")
    @OperLog(title = "合同管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@RequestBody @Valid BizContractForm form) {
        return R.ok(contractService.create(form));
    }

    @PreAuthorize("@ps.has('biz:contract:edit')")
    @OperLog(title = "合同管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@RequestBody @Valid BizContractForm form) {
        contractService.update(form);
        return R.ok();
    }

    @PreAuthorize("@ps.has('biz:contract:remove')")
    @OperLog(title = "合同管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<Long> ids) {
        contractService.remove(ids);
        return R.ok();
    }
}
