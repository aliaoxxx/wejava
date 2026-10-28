package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.system.converter.SysDictTypeConverter;
import com.wejava.system.mapper.SysDictDataMapper;
import com.wejava.system.mapper.SysDictTypeMapper;
import com.wejava.system.model.entity.SysDictData;
import com.wejava.system.model.entity.SysDictType;
import com.wejava.system.model.form.SysDictTypeForm;
import com.wejava.system.model.query.SysDictTypeQuery;
import com.wejava.system.model.vo.SysDictTypeVO;
import com.wejava.system.service.SysDictTypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl implements SysDictTypeService {

    private final SysDictTypeMapper typeMapper;
    private final SysDictDataMapper dataMapper;
    private final SysDictTypeConverter converter;

    @Override
    public PageResult<SysDictTypeVO> page(SysDictTypeQuery query) {
        Page<SysDictType> page = query.toPage();
        Page<SysDictType> result = typeMapper.selectPage(
                page,
                Wrappers.<SysDictType>lambdaQuery()
                        .like(StringUtils.hasText(query.getDictName()), SysDictType::getDictName, query.getDictName())
                        .like(StringUtils.hasText(query.getDictType()), SysDictType::getDictType, query.getDictType())
                        .eq(query.getStatus() != null, SysDictType::getStatus, query.getStatus())
                        .orderByDesc(SysDictType::getDictId));
        return PageResult.of(result.convert(converter::toVO));
    }

    @Override
    public SysDictTypeVO getById(Long dictId) {
        return converter.toVO(requireType(dictId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysDictTypeForm form) {
        if (typeMapper.exists(Wrappers.<SysDictType>lambdaQuery().eq(SysDictType::getDictType, form.getDictType()))) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS);
        }
        SysDictType entity = converter.toEntity(form);
        entity.setDictId(null);
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        typeMapper.insert(entity);
        return entity.getDictId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysDictTypeForm form) {
        requireType(form.getDictId());
        boolean duplicated = typeMapper.exists(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getDictType, form.getDictType())
                .ne(SysDictType::getDictId, form.getDictId()));
        if (duplicated) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS);
        }
        SysDictType entity = requireType(form.getDictId());
        String oldType = entity.getDictType();
        converter.update(form, entity);
        typeMapper.updateById(entity);

        // 字典类型改了，下面的数据要跟着改，否则会成为孤儿
        if (!oldType.equals(entity.getDictType())) {
            dataMapper.update(
                    null,
                    Wrappers.<SysDictData>lambdaUpdate()
                            .eq(SysDictData::getDictType, oldType)
                            .set(SysDictData::getDictType, entity.getDictType()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> dictIds) {
        for (Long dictId : dictIds) {
            SysDictType type = typeMapper.selectById(dictId);
            if (type == null) {
                continue;
            }
            long dataCount = dataMapper.selectCount(
                    Wrappers.<SysDictData>lambdaQuery().eq(SysDictData::getDictType, type.getDictType()));
            if (dataCount > 0) {
                throw new BizException(ErrorCode.HAS_REFERENCE.getCode(), "字典「" + type.getDictName() + "」下还有数据，无法删除");
            }
            typeMapper.deleteById(dictId);
        }
    }

    @Override
    public List<SysDictTypeVO> optionSelect() {
        return converter.toVOList(typeMapper.selectList(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getStatus, 0)
                .orderByDesc(SysDictType::getDictId)));
    }

    private SysDictType requireType(Long dictId) {
        SysDictType type = typeMapper.selectById(dictId);
        if (type == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return type;
    }
}
