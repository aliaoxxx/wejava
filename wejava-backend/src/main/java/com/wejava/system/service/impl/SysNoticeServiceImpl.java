package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.system.converter.SysNoticeConverter;
import com.wejava.system.mapper.SysNoticeMapper;
import com.wejava.system.model.entity.SysNotice;
import com.wejava.system.model.form.SysNoticeForm;
import com.wejava.system.model.query.SysNoticeQuery;
import com.wejava.system.model.vo.SysNoticeVO;
import com.wejava.system.service.SysNoticeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl implements SysNoticeService {

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeConverter converter;

    @Override
    public PageResult<SysNoticeVO> page(SysNoticeQuery query) {
        Page<SysNotice> page = query.toPage();
        Page<SysNotice> result = noticeMapper.selectPage(
                page,
                Wrappers.<SysNotice>lambdaQuery()
                        .like(
                                StringUtils.hasText(query.getNoticeTitle()),
                                SysNotice::getNoticeTitle,
                                query.getNoticeTitle())
                        .eq(query.getNoticeType() != null, SysNotice::getNoticeType, query.getNoticeType())
                        .eq(query.getStatus() != null, SysNotice::getStatus, query.getStatus())
                        .orderByDesc(SysNotice::getNoticeId));
        return PageResult.of(result.convert(converter::toVO));
    }

    @Override
    public SysNoticeVO getById(Long noticeId) {
        return converter.toVO(requireNotice(noticeId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysNoticeForm form) {
        SysNotice entity = converter.toEntity(form);
        entity.setNoticeId(null);
        if (entity.getNoticeType() == null) {
            entity.setNoticeType(1);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        noticeMapper.insert(entity);
        return entity.getNoticeId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysNoticeForm form) {
        SysNotice entity = requireNotice(form.getNoticeId());
        converter.update(form, entity);
        noticeMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> noticeIds) {
        noticeMapper.deleteByIds(noticeIds);
    }

    private SysNotice requireNotice(Long noticeId) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return notice;
    }
}
