package ${table.packageName}.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import ${table.packageName}.converter.${table.className}Converter;
import ${table.packageName}.mapper.${table.className}Mapper;
import ${table.packageName}.model.entity.${table.className};
import ${table.packageName}.model.form.${table.className}Form;
import ${table.packageName}.model.query.${table.className}Query;
import ${table.packageName}.model.vo.${table.className}VO;
import ${table.packageName}.service.${table.className}Service;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
                        .ge(query.getBegin${col.javaFieldCap}() != null, ${table.className}::get${col.javaFieldCap}, query.getBegin${col.javaFieldCap}())
                        .le(query.getEnd${col.javaFieldCap}() != null, ${table.className}::get${col.javaFieldCap}, query.getEnd${col.javaFieldCap}())
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
