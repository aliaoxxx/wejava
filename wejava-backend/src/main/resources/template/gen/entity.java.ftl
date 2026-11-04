package ${table.packageName}.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wejava.common.core.BaseEntity;
<#list entityImports as imp>
import ${imp};
</#list>
import lombok.Data;
import lombok.EqualsAndHashCode;

/** ${table.functionName} */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("${table.tableName}")
public class ${table.className} extends BaseEntity {
<#if table.pkColumn??>

    @TableId(value = "${table.pkColumn.columnName}", type = IdType.AUTO)
    private ${table.pkColumn.javaType} ${table.pkColumn.javaField};
</#if>
<#list columns as col>
<#if !col.isPk && !col.audit>

    /** ${col.columnComment} */
    private ${col.javaType} ${col.javaField};
</#if>
</#list>
}
