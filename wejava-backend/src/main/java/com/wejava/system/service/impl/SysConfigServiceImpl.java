package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.system.converter.SysConfigConverter;
import com.wejava.system.mapper.SysConfigMapper;
import com.wejava.system.model.entity.SysConfig;
import com.wejava.system.model.form.SysConfigForm;
import com.wejava.system.model.query.SysConfigQuery;
import com.wejava.system.model.vo.SysConfigVO;
import com.wejava.system.service.SysConfigService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper configMapper;
    private final SysConfigConverter converter;

    @Override
    public PageResult<SysConfigVO> page(SysConfigQuery query) {
        Page<SysConfig> page = query.toPage();
        Page<SysConfig> result = configMapper.selectPage(
                page,
                Wrappers.<SysConfig>lambdaQuery()
                        .like(
                                StringUtils.hasText(query.getConfigName()),
                                SysConfig::getConfigName,
                                query.getConfigName())
                        .like(StringUtils.hasText(query.getConfigKey()), SysConfig::getConfigKey, query.getConfigKey())
                        .eq(query.getConfigType() != null, SysConfig::getConfigType, query.getConfigType())
                        .orderByDesc(SysConfig::getConfigId));
        return PageResult.of(result.convert(converter::toVO));
    }

    @Override
    public SysConfigVO getById(Long configId) {
        return converter.toVO(requireConfig(configId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysConfigForm form) {
        if (configMapper.exists(Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, form.getConfigKey()))) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS);
        }
        SysConfig entity = converter.toEntity(form);
        entity.setConfigId(null);
        if (entity.getConfigType() == null) {
            entity.setConfigType(0);
        }
        configMapper.insert(entity);
        return entity.getConfigId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysConfigForm form) {
        requireConfig(form.getConfigId());
        boolean duplicated = configMapper.exists(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, form.getConfigKey())
                .ne(SysConfig::getConfigId, form.getConfigId()));
        if (duplicated) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS);
        }
        SysConfig entity = requireConfig(form.getConfigId());
        converter.update(form, entity);
        configMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> configIds) {
        for (Long configId : configIds) {
            SysConfig config = configMapper.selectById(configId);
            if (config == null) {
                continue;
            }
            if (Integer.valueOf(1).equals(config.getConfigType())) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "「" + config.getConfigName() + "」是内置参数，不允许删除");
            }
            configMapper.deleteById(configId);
        }
    }

    @Override
    public String getValueByKey(String configKey) {
        SysConfig config =
                configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, configKey));
        if (config == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return config.getConfigValue();
    }

    private SysConfig requireConfig(Long configId) {
        SysConfig config = configMapper.selectById(configId);
        if (config == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return config;
    }
}
