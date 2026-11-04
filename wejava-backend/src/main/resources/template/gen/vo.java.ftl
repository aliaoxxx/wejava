package ${table.packageName}.model.vo;

<#list voImports as imp>
import ${imp};
</#list>
import lombok.Data;

/** ${table.functionName} */
@Data
public class ${table.className}VO {
<#list columns as col>

    /** ${col.columnComment} */
    private ${col.javaType} ${col.javaField};
</#list>
}
