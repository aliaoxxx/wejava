package ${table.packageName}.mapper;

<#assign mapperImports = [
    "com.baomidou.mybatisplus.core.mapper.BaseMapper",
    table.packageName + ".model.entity." + table.className,
    "org.apache.ibatis.annotations.Mapper"
]>
<#list mapperImports?sort as imp>
import ${imp};
</#list>

@Mapper
public interface ${table.className}Mapper extends BaseMapper<${table.className}> {}
