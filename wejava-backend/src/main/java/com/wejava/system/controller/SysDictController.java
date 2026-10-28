package com.wejava.system.controller;

import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.system.model.form.SysDictDataForm;
import com.wejava.system.model.form.SysDictTypeForm;
import com.wejava.system.model.query.SysDictDataQuery;
import com.wejava.system.model.query.SysDictTypeQuery;
import com.wejava.system.model.vo.SysDictDataVO;
import com.wejava.system.model.vo.SysDictTypeVO;
import com.wejava.system.service.SysDictDataService;
import com.wejava.system.service.SysDictTypeService;
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
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictTypeService typeService;
    private final SysDictDataService dataService;

    // ---------------- 字典类型 ----------------

    @PreAuthorize("@ps.has('system:dict:list')")
    @GetMapping("/type/list")
    public R<PageResult<SysDictTypeVO>> typeList(SysDictTypeQuery query) {
        return R.ok(typeService.page(query));
    }

    @PreAuthorize("@ps.has('system:dict:query')")
    @GetMapping("/type/{dictId}")
    public R<SysDictTypeVO> typeGet(@PathVariable Long dictId) {
        return R.ok(typeService.getById(dictId));
    }

    @PreAuthorize("@ps.has('system:dict:add')")
    @PostMapping("/type")
    public R<Long> typeCreate(@RequestBody @Valid SysDictTypeForm form) {
        return R.ok(typeService.create(form));
    }

    @PreAuthorize("@ps.has('system:dict:edit')")
    @PutMapping("/type")
    public R<Void> typeUpdate(@RequestBody @Valid SysDictTypeForm form) {
        typeService.update(form);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:dict:remove')")
    @DeleteMapping("/type/{dictIds}")
    public R<Void> typeRemove(@PathVariable List<Long> dictIds) {
        typeService.remove(dictIds);
        return R.ok();
    }

    /** 字典类型下拉：登录即可 */
    @GetMapping("/type/option-select")
    public R<List<SysDictTypeVO>> typeOptions() {
        return R.ok(typeService.optionSelect());
    }

    // ---------------- 字典数据 ----------------

    @PreAuthorize("@ps.has('system:dict:list')")
    @GetMapping("/data/list")
    public R<PageResult<SysDictDataVO>> dataList(SysDictDataQuery query) {
        return R.ok(dataService.page(query));
    }

    @PreAuthorize("@ps.has('system:dict:query')")
    @GetMapping("/data/{dictCode}")
    public R<SysDictDataVO> dataGet(@PathVariable Long dictCode) {
        return R.ok(dataService.getById(dictCode));
    }

    @PreAuthorize("@ps.has('system:dict:add')")
    @PostMapping("/data")
    public R<Long> dataCreate(@RequestBody @Valid SysDictDataForm form) {
        return R.ok(dataService.create(form));
    }

    @PreAuthorize("@ps.has('system:dict:edit')")
    @PutMapping("/data")
    public R<Void> dataUpdate(@RequestBody @Valid SysDictDataForm form) {
        dataService.update(form);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:dict:remove')")
    @DeleteMapping("/data/{dictCodes}")
    public R<Void> dataRemove(@PathVariable List<Long> dictCodes) {
        dataService.remove(dictCodes);
        return R.ok();
    }

    /** 按类型取字典数据：登录即可，前端下拉/表格标签都用它 */
    @GetMapping("/data/type/{dictType}")
    public R<List<SysDictDataVO>> dataByType(@PathVariable String dictType) {
        return R.ok(dataService.listByType(dictType));
    }
}
