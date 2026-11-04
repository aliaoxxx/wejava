package ${table.packageName}.converter;

import ${table.packageName}.model.entity.${table.className};
import ${table.packageName}.model.form.${table.className}Form;
import ${table.packageName}.model.vo.${table.className}VO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ${table.className}Converter {

    ${table.className}VO toVO(${table.className} entity);

    List<${table.className}VO> toVOList(List<${table.className}> entities);

    @Mapping(target = "${table.pkColumn.javaField}", ignore = true)
    ${table.className} toEntity(${table.className}Form form);

    @Mapping(target = "${table.pkColumn.javaField}", ignore = true)
    void update(${table.className}Form form, @MappingTarget ${table.className} entity);
}
