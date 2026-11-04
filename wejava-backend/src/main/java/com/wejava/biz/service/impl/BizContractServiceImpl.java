package com.wejava.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.biz.converter.BizContractConverter;
import com.wejava.biz.mapper.BizContractMapper;
import com.wejava.biz.model.entity.BizContract;
import com.wejava.biz.model.form.BizContractForm;
import com.wejava.biz.model.query.BizContractQuery;
import com.wejava.biz.model.vo.BizContractVO;
import com.wejava.biz.service.BizContractService;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 合同管理 */
@Service
@RequiredArgsConstructor
public class BizContractServiceImpl implements BizContractService {

    private final BizContractMapper contractMapper;
    private final BizContractConverter converter;

    @Override
    public PageResult<BizContractVO> page(BizContractQuery query) {
        Page<BizContract> page = query.toPage();
        Page<BizContract> result = contractMapper.selectPage(
                page,
                Wrappers.<BizContract>lambdaQuery()
                        .like(
                                StringUtils.hasText(query.getContractNo()),
                                BizContract::getContractNo,
                                query.getContractNo())
                        .like(
                                StringUtils.hasText(query.getContractName()),
                                BizContract::getContractName,
                                query.getContractName())
                        .eq(
                                StringUtils.hasText(query.getContractType()),
                                BizContract::getContractType,
                                query.getContractType())
                        .ge(query.getBeginSignDate() != null, BizContract::getSignDate, query.getBeginSignDate())
                        .le(query.getEndSignDate() != null, BizContract::getSignDate, query.getEndSignDate())
                        .ge(query.getBeginExpireTime() != null, BizContract::getExpireTime, query.getBeginExpireTime())
                        .le(query.getEndExpireTime() != null, BizContract::getExpireTime, query.getEndExpireTime())
                        .eq(query.getStatus() != null, BizContract::getStatus, query.getStatus())
                        .orderByDesc(BizContract::getContractId));
        return PageResult.of(result.convert(converter::toVO));
    }

    @Override
    public BizContractVO getById(Long id) {
        return converter.toVO(require(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BizContractForm form) {
        BizContract entity = converter.toEntity(form);
        entity.setContractId(null);
        contractMapper.insert(entity);
        return entity.getContractId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BizContractForm form) {
        BizContract entity = require(form.getContractId());
        converter.update(form, entity);
        contractMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> ids) {
        contractMapper.deleteByIds(ids);
    }

    private BizContract require(Long id) {
        BizContract entity = contractMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return entity;
    }
}
