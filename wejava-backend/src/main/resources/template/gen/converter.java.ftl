package ${table.packageName}.converter;

<#assign converterImports = [
    table.packageName + ".model.entity." + table.className,
    table.packageName + ".model.form." + table.className + "Form",
    table.packageName + ".model.vo." + table.className + "VO",
    "java.util.List",
    "org.mapstruct.Mapper",
    "org.mapstruct.Mapping",
    "org.mapstruct.MappingTarget",
    "org.mapstruct.NullValuePropertyMappingStrategy"
]>
<#list converterImports?sort as imp>
import ${imp};
</#list>

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ${table.className}Converter {

    ${table.className}VO toVO(${table.className} entity);

    List<${table.className}VO> toVOList(List<${table.className}> entities);

    @Mapping(target = "${table.pkColumn.javaField}", ignore = true)
    ${table.className} toEntity(${table.className}Form form);

    @Mapping(target = "${table.pkColumn.javaField}", ignore = true)
    void update(${table.className}Form form, @MappingTarget ${table.className} entity);
}
