package ${table.packageName}.model.form;

<#if hasNotBlank>
import jakarta.validation.constraints.NotBlank;
</#if>
<#if hasNotNull>
import jakarta.validation.constraints.NotNull;
</#if>
<#if hasSize>
import jakarta.validation.constraints.Size;
</#if>
<#list formImports as imp>
import ${imp};
</#list>
import lombok.Data;

/** ${table.functionName} */
@Data
public class ${table.className}Form {
<#if table.pkColumn??>

    private ${table.pkColumn.javaType} ${table.pkColumn.javaField};
</#if>
<#list columns as col>
<#if !col.isPk && !col.audit>

    /** ${col.columnComment} */
<#if col.required>
<#if col.javaType == "String">
    @NotBlank(message = "${col.columnComment}不能为空")
<#else>
    @NotNull(message = "${col.columnComment}不能为空")
</#if>
</#if>
<#if col.javaType == "String" && col.maxLength gt 0>
    @Size(max = ${col.maxLength}, message = "${col.columnComment}最长 ${col.maxLength}")
</#if>
    private ${col.javaType} ${col.javaField};
</#if>
</#list>
}
