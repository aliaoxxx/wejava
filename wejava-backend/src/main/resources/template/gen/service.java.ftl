package ${table.packageName}.service;

<#assign serviceImports = [
    "com.wejava.common.core.PageResult",
    table.packageName + ".model.form." + table.className + "Form",
    table.packageName + ".model.query." + table.className + "Query",
    table.packageName + ".model.vo." + table.className + "VO",
    "java.util.List"
]>
<#list serviceImports?sort as imp>
import ${imp};
</#list>

/** ${table.functionName} */
public interface ${table.className}Service {

    PageResult<${table.className}VO> page(${table.className}Query query);

    ${table.className}VO getById(${table.pkColumn.javaType} ${table.pkColumn.javaField});

    ${table.pkColumn.javaType} create(${table.className}Form form);

    void update(${table.className}Form form);

    void remove(List<${table.pkColumn.javaType}> ids);
}
