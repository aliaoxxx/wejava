package ${table.packageName}.controller;

import com.wejava.common.annotation.BusinessType;
import com.wejava.common.annotation.OperLog;
import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import ${table.packageName}.model.form.${table.className}Form;
import ${table.packageName}.model.query.${table.className}Query;
import ${table.packageName}.model.vo.${table.className}VO;
import ${table.packageName}.service.${table.className}Service;
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

/** ${table.functionName} */
@RestController
@RequestMapping("${table.routePath}")
@RequiredArgsConstructor
public class ${table.className}Controller {

    private final ${table.className}Service ${table.businessName}Service;

    @PreAuthorize("@ps.has('${table.permissionPrefix}:list')")
    @GetMapping("/list")
    public R<PageResult<${table.className}VO>> list(${table.className}Query query) {
        return R.ok(${table.businessName}Service.page(query));
    }

    @PreAuthorize("@ps.has('${table.permissionPrefix}:query')")
    @GetMapping("/{${table.pkColumn.javaField}}")
    public R<${table.className}VO> getById(@PathVariable ${table.pkColumn.javaType} ${table.pkColumn.javaField}) {
        return R.ok(${table.businessName}Service.getById(${table.pkColumn.javaField}));
    }

    @PreAuthorize("@ps.has('${table.permissionPrefix}:add')")
    @OperLog(title = "${table.functionName}", businessType = BusinessType.INSERT)
    @PostMapping
    public R<${table.pkColumn.javaType}> create(@RequestBody @Valid ${table.className}Form form) {
        return R.ok(${table.businessName}Service.create(form));
    }

    @PreAuthorize("@ps.has('${table.permissionPrefix}:edit')")
    @OperLog(title = "${table.functionName}", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@RequestBody @Valid ${table.className}Form form) {
        ${table.businessName}Service.update(form);
        return R.ok();
    }

    @PreAuthorize("@ps.has('${table.permissionPrefix}:remove')")
    @OperLog(title = "${table.functionName}", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable List<${table.pkColumn.javaType}> ids) {
        ${table.businessName}Service.remove(ids);
        return R.ok();
    }
}
