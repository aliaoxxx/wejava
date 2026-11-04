package ${table.packageName}.model.query;

import com.wejava.common.core.PageQuery;
<#list queryImports as imp>
import ${imp};
</#list>
import lombok.Data;
import lombok.EqualsAndHashCode;

/** ${table.functionName}查询条件 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ${table.className}Query extends PageQuery {
<#list queryColumns as col>
<#if col.queryType == "BETWEEN">

    /** ${col.columnComment}起 */
    private ${col.javaType} begin${col.javaFieldCap};

    /** ${col.columnComment}止 */
    private ${col.javaType} end${col.javaFieldCap};
<#else>

    /** ${col.columnComment} */
    private ${col.javaType} ${col.javaField};
</#if>
</#list>
}
