package ${table.packageName}.service.impl;

<#assign implImports = [
    "com.baomidou.mybatisplus.core.toolkit.Wrappers",
    "com.baomidou.mybatisplus.extension.plugins.pagination.Page",
    "com.wejava.common.core.PageResult",
    "com.wejava.common.exception.BizException",
    "com.wejava.common.exception.ErrorCode",
    table.packageName + ".converter." + table.className + "Converter",
    table.packageName + ".mapper." + table.className + "Mapper",
    table.packageName + ".model.entity." + table.className,
    table.packageName + ".model.form." + table.className + "Form",
    table.packageName + ".model.query." + table.className + "Query",
    table.packageName + ".model.vo." + table.className + "VO",
    table.packageName + ".service." + table.className + "Service",
    "java.util.List",
    "lombok.RequiredArgsConstructor",
    "org.springframework.stereotype.Service",
    "org.springframework.transaction.annotation.Transactional",
    "org.springframework.util.StringUtils"
]>
<#list implImports?sort as imp>
import ${imp};
</#list>

/** ${table.functionName} */
@Service
@RequiredArgsConstructor
public class ${table.className}ServiceImpl implements ${table.className}Service {

    private final ${table.className}Mapper ${table.businessName}Mapper;
    private final ${table.className}Converter converter;

    @Override
    public PageResult<${table.className}VO> page(${table.className}Query query) {
        Page<${table.className}> page = query.toPage();
        Page<${table.className}> result = ${table.businessName}Mapper.selectPage(
                page,
                Wrappers.<${table.className}>lambdaQuery()
<#list queryColumns as col>
<#if col.queryType == "BETWEEN">
<#if col.wrapBetweenCall>
                        .ge(
                                query.getBegin${col.javaFieldCap}() != null,
                                ${table.className}::get${col.javaFieldCap},
                                query.getBegin${col.javaFieldCap}())
                        .le(
                                query.getEnd${col.javaFieldCap}() != null,
                                ${table.className}::get${col.javaFieldCap},
                                query.getEnd${col.javaFieldCap}())
<#else>
                        .ge(query.getBegin${col.javaFieldCap}() != null, ${table.className}::get${col.javaFieldCap}, query.getBegin${col.javaFieldCap}())
                        .le(query.getEnd${col.javaFieldCap}() != null, ${table.className}::get${col.javaFieldCap}, query.getEnd${col.javaFieldCap}())
</#if>
<#elseif col.javaType == "String">
<#if col.wrapStringCall>
                        .${col.queryType?lower_case}(
                                StringUtils.hasText(query.get${col.javaFieldCap}()),
                                ${table.className}::get${col.javaFieldCap},
                                query.get${col.javaFieldCap}())
<#else>
                        .${col.queryType?lower_case}(StringUtils.hasText(query.get${col.javaFieldCap}()), ${table.className}::get${col.javaFieldCap}, query.get${col.javaFieldCap}())
</#if>
<#else>
<#if col.wrapNumberCall>
                        .eq(
                                query.get${col.javaFieldCap}() != null,
                                ${table.className}::get${col.javaFieldCap},
                                query.get${col.javaFieldCap}())
<#else>
                        .eq(query.get${col.javaFieldCap}() != null, ${table.className}::get${col.javaFieldCap}, query.get${col.javaFieldCap}())
</#if>
</#if>
</#list>
                        .orderByDesc(${table.className}::get${table.pkColumn.javaFieldCap}));
        return PageResult.of(result.convert(converter::toVO));
    }

    @Override
    public ${table.className}VO getById(${table.pkColumn.javaType} id) {
        return converter.toVO(require(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ${table.pkColumn.javaType} create(${table.className}Form form) {
        ${table.className} entity = converter.toEntity(form);
        entity.set${table.pkColumn.javaFieldCap}(null);
        ${table.businessName}Mapper.insert(entity);
        return entity.get${table.pkColumn.javaFieldCap}();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(${table.className}Form form) {
        ${table.className} entity = require(form.get${table.pkColumn.javaFieldCap}());
        converter.update(form, entity);
        ${table.businessName}Mapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<${table.pkColumn.javaType}> ids) {
        ${table.businessName}Mapper.deleteByIds(ids);
    }

    private ${table.className} require(${table.pkColumn.javaType} id) {
        ${table.className} entity = ${table.businessName}Mapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return entity;
    }
}
