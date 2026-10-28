package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.system.converter.SysDictDataConverter;
import com.wejava.system.mapper.SysDictDataMapper;
import com.wejava.system.model.entity.SysDictData;
import com.wejava.system.model.form.SysDictDataForm;
import com.wejava.system.model.query.SysDictDataQuery;
import com.wejava.system.model.vo.SysDictDataVO;
import com.wejava.system.service.SysDictDataService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl implements SysDictDataService {

    private final SysDictDataMapper dataMapper;
    private final SysDictDataConverter converter;

    @Override
    public PageResult<SysDictDataVO> page(SysDictDataQuery query) {
        Page<SysDictData> page = query.toPage();
        Page<SysDictData> result = dataMapper.selectPage(
                page,
                Wrappers.<SysDictData>lambdaQuery()
                        .like(
                                StringUtils.hasText(query.getDictLabel()),
                                SysDictData::getDictLabel,
                                query.getDictLabel())
                        .eq(StringUtils.hasText(query.getDictType()), SysDictData::getDictType, query.getDictType())
                        .eq(query.getStatus() != null, SysDictData::getStatus, query.getStatus())
                        .orderByAsc(SysDictData::getDictSort));
        return PageResult.of(result.convert(converter::toVO));
    }

    @Override
    public SysDictDataVO getById(Long dictCode) {
        return converter.toVO(requireData(dictCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysDictDataForm form) {
        SysDictData entity = converter.toEntity(form);
        entity.setDictCode(null);
        if (entity.getDictSort() == null) {
            entity.setDictSort(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        dataMapper.insert(entity);
        return entity.getDictCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysDictDataForm form) {
        SysDictData entity = requireData(form.getDictCode());
        converter.update(form, entity);
        dataMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> dictCodes) {
        dataMapper.deleteByIds(dictCodes);
    }

    @Override
    public List<SysDictDataVO> listByType(String dictType) {
        return converter.toVOList(dataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 0)
                .orderByAsc(SysDictData::getDictSort)));
    }

    private SysDictData requireData(Long dictCode) {
        SysDictData data = dataMapper.selectById(dictCode);
        if (data == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return data;
    }
}
