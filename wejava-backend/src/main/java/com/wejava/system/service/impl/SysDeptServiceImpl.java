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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements SysDeptService {

    /** 部门树最大节点数，超了说明数据异常（成环） */
    private static final int MAX_TREE_NODES = 10000;

    /** 部门树最大深度 */
    private static final int MAX_TREE_DEPTH = 32;

    private final SysDeptMapper deptMapper;
    private final SysDeptConverter converter;

    @Override
    public List<SysDeptVO> tree(SysDeptQuery query) {
        List<SysDept> depts = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .like(StringUtils.hasText(query.getDeptName()), SysDept::getDeptName, query.getDeptName())
                .eq(query.getStatus() != null, SysDept::getStatus, query.getStatus())
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getOrderNum));
        return buildTree(converter.toVOList(depts));
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
        return buildTree(converter.toVOList(depts));
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

    /**
     * 刷新子孙部门的祖级链。
     * <p>
     * 原来每个子部门都要 SELECT 一次、UPDATE 一次。现在一次查出全部部门在内存里算好，
     * 再用一条 CASE WHEN 批量更新，查询次数从 O(节点数) 降到 2 次。
     */
    private void refreshChildrenAncestors(SysDept parent) {
        List<SysDept> all =
                deptMapper.selectList(Wrappers.<SysDept>lambdaQuery().select(SysDept::getDeptId, SysDept::getParentId));
        Map<Long, List<SysDept>> byParent = all.stream()
                .filter(dept -> dept.getParentId() != null)
                .collect(Collectors.groupingBy(SysDept::getParentId));

        List<SysDept> updates = new ArrayList<>();
        Deque<Object[]> queue = new ArrayDeque<>();
        queue.add(new Object[] {parent, parent.getAncestors()});

        int visited = 0;
        while (!queue.isEmpty()) {
            if (++visited > MAX_TREE_NODES) {
                // 数据成环时防止死循环
                throw new IllegalStateException("部门层级异常，可能存在循环引用");
            }
            Object[] current = queue.poll();
            SysDept node = (SysDept) current[0];
            String nodeAncestors = (String) current[1];
            for (SysDept child : byParent.getOrDefault(node.getDeptId(), List.of())) {
                String childAncestors = nodeAncestors + "," + node.getDeptId();
                SysDept update = new SysDept();
                update.setDeptId(child.getDeptId());
                update.setAncestors(childAncestors);
                updates.add(update);
                queue.add(new Object[] {child, childAncestors});
            }
        }
        if (!updates.isEmpty()) {
            deptMapper.updateAncestorsBatch(updates);
        }
    }

    /** 一次分组后按层挂载，避免每个节点都遍历全量列表（O(n²) → O(n)） */
    private List<SysDeptVO> buildTree(List<SysDeptVO> all) {
        Map<Long, List<SysDeptVO>> byParent = all.stream()
                .collect(Collectors.groupingBy(node -> node.getParentId() == null ? 0L : node.getParentId()));
        return attachChildren(byParent, 0L, 0);
    }

    private List<SysDeptVO> attachChildren(Map<Long, List<SysDeptVO>> byParent, Long parentId, int depth) {
        if (depth > MAX_TREE_DEPTH) {
            throw new IllegalStateException("部门层级过深，可能存在循环引用");
        }
        List<SysDeptVO> nodes = byParent.getOrDefault(parentId, List.of());
        for (SysDeptVO node : nodes) {
            node.setChildren(attachChildren(byParent, node.getDeptId(), depth + 1));
        }
        return nodes;
    }

    private SysDept requireDept(Long deptId) {
        SysDept dept = deptMapper.selectById(deptId);
        if (dept == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return dept;
    }
}
