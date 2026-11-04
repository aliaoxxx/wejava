package ${table.packageName}.model.query;

<#assign hasDate = false>
<#list queryColumns as col>
<#if col.javaType == "LocalDateTime" || col.javaType == "LocalDate">
<#assign hasDate = true>
</#if>
</#list>
<#assign queryImportsWithDate = queryImports + [
    "com.wejava.common.core.PageQuery",
    "lombok.Data",
    "lombok.EqualsAndHashCode"
]>
<#if hasDate>
<#assign queryImportsWithDate = queryImportsWithDate + ["org.springframework.format.annotation.DateTimeFormat"]>
</#if>
<#list queryImportsWithDate?sort as imp>
import ${imp};
</#list>

/** ${table.functionName}查询条件 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ${table.className}Query extends PageQuery {
<#list queryColumns as col>
<#if col.queryType == "BETWEEN">

    /** ${col.columnComment}起 */
<#if col.javaType == "LocalDateTime">
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
<#elseif col.javaType == "LocalDate">
    @DateTimeFormat(pattern = "yyyy-MM-dd")
</#if>
    private ${col.javaType} begin${col.javaFieldCap};

    /** ${col.columnComment}止 */
<#if col.javaType == "LocalDateTime">
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
<#elseif col.javaType == "LocalDate">
    @DateTimeFormat(pattern = "yyyy-MM-dd")
</#if>
    private ${col.javaType} end${col.javaFieldCap};
<#else>

    /** ${col.columnComment} */
    private ${col.javaType} ${col.javaField};
</#if>
</#list>
}
