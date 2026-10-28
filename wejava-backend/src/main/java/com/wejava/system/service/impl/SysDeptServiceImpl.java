package com.wejava.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.system.converter.SysDeptConverter;
import com.wejava.system.mapper.SysDeptMapper;
import com.wejava.system.model.entity.SysDept;
import com.wejava.system.model.form.SysDeptForm;
import com.wejava.system.model.query.SysDeptQuery;
import com.wejava.system.model.vo.SysDeptVO;
import com.wejava.system.service.SysDeptService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper deptMapper;
    private final SysDeptConverter converter;

    @Override
    public List<SysDeptVO> tree(SysDeptQuery query) {
        List<SysDept> depts = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .like(StringUtils.hasText(query.getDeptName()), SysDept::getDeptName, query.getDeptName())
                .eq(query.getStatus() != null, SysDept::getStatus, query.getStatus())
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getOrderNum));
        return buildTree(converter.toVOList(depts), 0L);
    }

    @Override
    public SysDeptVO getById(Long deptId) {
        return converter.toVO(requireDept(deptId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysDeptForm form) {
        SysDept entity = converter.toEntity(form);
        entity.setDeptId(null);
        entity.setAncestors(buildAncestors(form.getParentId()));
        if (entity.getOrderNum() == null) {
            entity.setOrderNum(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        deptMapper.insert(entity);
        return entity.getDeptId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysDeptForm form) {
        SysDept entity = requireDept(form.getDeptId());
        checkParent(form.getDeptId(), form.getParentId());

        converter.update(form, entity);
        entity.setAncestors(buildAncestors(form.getParentId()));
        deptMapper.updateById(entity);

        // 父级变了，子孙的祖级链要一起刷新
        refreshChildrenAncestors(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long deptId) {
        requireDept(deptId);
        long children = deptMapper.selectCount(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, deptId));
        if (children > 0) {
            throw new BizException(ErrorCode.HAS_CHILDREN);
        }
        if (deptMapper.countUsersByDeptId(deptId) > 0) {
            throw new BizException(ErrorCode.DEPT_HAS_USER);
        }
        deptMapper.deleteById(deptId);
    }

    @Override
    public List<SysDeptVO> treeSelect() {
        List<SysDept> depts = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getStatus, 0)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getOrderNum));
        return buildTree(converter.toVOList(depts), 0L);
    }

    // ---------------- 内部 ----------------

    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysDept parent = requireDept(parentId);
        return parent.getAncestors() + "," + parent.getDeptId();
    }

    /** 不能选自己，也不能选自己的子孙 */
    private void checkParent(Long deptId, Long parentId) {
        if (Objects.equals(deptId, parentId)) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "上级部门不能是自己");
        }
        if (parentId == null || parentId == 0L) {
            return;
        }
        SysDept parent = requireDept(parentId);
        if (StringUtils.hasText(parent.getAncestors())
                && Arrays.asList(parent.getAncestors().split(",")).contains(String.valueOf(deptId))) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "上级部门不能是自己的下级");
        }
    }

    private void refreshChildrenAncestors(SysDept parent) {
        List<SysDept> children =
                deptMapper.selectList(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, parent.getDeptId()));
        for (SysDept child : children) {
            child.setAncestors(parent.getAncestors() + "," + parent.getDeptId());
            deptMapper.updateById(child);
            refreshChildrenAncestors(child);
        }
    }

    private List<SysDeptVO> buildTree(List<SysDeptVO> all, Long parentId) {
        List<SysDeptVO> result = new ArrayList<>();
        for (SysDeptVO node : all) {
            if (!Objects.equals(node.getParentId(), parentId)) {
                continue;
            }
            node.setChildren(buildTree(all, node.getDeptId()));
            result.add(node);
        }
        return result;
    }

    private SysDept requireDept(Long deptId) {
        SysDept dept = deptMapper.selectById(deptId);
        if (dept == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return dept;
    }
}
