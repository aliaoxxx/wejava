package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.system.converter.SysPostConverter;
import com.wejava.system.mapper.SysPostMapper;
import com.wejava.system.model.entity.SysPost;
import com.wejava.system.model.form.SysPostForm;
import com.wejava.system.model.query.SysPostQuery;
import com.wejava.system.model.vo.PostOptionVO;
import com.wejava.system.model.vo.SysPostVO;
import com.wejava.system.service.SysPostService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysPostServiceImpl implements SysPostService {

    private final SysPostMapper postMapper;
    private final SysPostConverter converter;

    @Override
    public PageResult<SysPostVO> page(SysPostQuery query) {
        Page<SysPost> page = query.toPage();
        Page<SysPost> result = postMapper.selectPage(
                page,
                Wrappers.<SysPost>lambdaQuery()
                        .like(StringUtils.hasText(query.getPostCode()), SysPost::getPostCode, query.getPostCode())
                        .like(StringUtils.hasText(query.getPostName()), SysPost::getPostName, query.getPostName())
                        .eq(query.getStatus() != null, SysPost::getStatus, query.getStatus())
                        .orderByAsc(SysPost::getPostSort));
        return PageResult.of(result.convert(converter::toVO));
    }

    @Override
    public SysPostVO getById(Long postId) {
        return converter.toVO(requirePost(postId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysPostForm form) {
        if (postMapper.exists(Wrappers.<SysPost>lambdaQuery().eq(SysPost::getPostCode, form.getPostCode()))) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS);
        }
        SysPost entity = converter.toEntity(form);
        entity.setPostId(null);
        if (entity.getPostSort() == null) {
            entity.setPostSort(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        postMapper.insert(entity);
        return entity.getPostId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysPostForm form) {
        requirePost(form.getPostId());
        boolean duplicated = postMapper.exists(Wrappers.<SysPost>lambdaQuery()
                .eq(SysPost::getPostCode, form.getPostCode())
                .ne(SysPost::getPostId, form.getPostId()));
        if (duplicated) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS);
        }
        SysPost entity = requirePost(form.getPostId());
        converter.update(form, entity);
        postMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> postIds) {
        postMapper.deleteByIds(postIds);
    }

    @Override
    public List<PostOptionVO> optionSelect() {
        return postMapper
                .selectList(Wrappers.<SysPost>lambdaQuery()
                        .eq(SysPost::getStatus, 0)
                        .orderByAsc(SysPost::getPostSort))
                .stream()
                .map(post -> new PostOptionVO(post.getPostId(), post.getPostName()))
                .toList();
    }

    private SysPost requirePost(Long postId) {
        SysPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return post;
    }
}
