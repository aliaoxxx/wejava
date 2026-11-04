package ${table.packageName}.controller;

<#assign controllerImports = [
    "com.wejava.common.annotation.BusinessType",
    "com.wejava.common.annotation.OperLog",
    "com.wejava.common.core.PageResult",
    "com.wejava.common.core.R",
    table.packageName + ".model.form." + table.className + "Form",
    table.packageName + ".model.query." + table.className + "Query",
    table.packageName + ".model.vo." + table.className + "VO",
    table.packageName + ".service." + table.className + "Service",
    "jakarta.validation.Valid",
    "java.util.List",
    "lombok.RequiredArgsConstructor",
    "org.springframework.security.access.prepost.PreAuthorize",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PathVariable",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.RequestBody",
    "org.springframework.web.bind.annotation.RequestMapping",
    "org.springframework.web.bind.annotation.RestController"
]>
<#list controllerImports?sort as imp>
import ${imp};
</#list>

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
