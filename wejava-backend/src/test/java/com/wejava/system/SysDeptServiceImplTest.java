package com.wejava.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wejava.common.exception.BizException;
import com.wejava.system.mapper.SysDeptMapper;
import com.wejava.system.model.entity.SysDept;
import com.wejava.system.model.form.SysDeptForm;
import com.wejava.system.model.query.SysDeptQuery;
import com.wejava.system.model.vo.SysDeptVO;
import com.wejava.system.service.SysDeptService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 部门树的祖级链维护 —— 覆盖移动部门后的批量刷新逻辑 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SysDeptServiceImplTest {

    @Autowired
    private SysDeptService deptService;

    @Autowired
    private SysDeptMapper deptMapper;

    private Long create(String name, Long parentId) {
        SysDeptForm form = new SysDeptForm();
        form.setDeptName(name);
        form.setParentId(parentId);
        return deptService.create(form);
    }

    private String ancestorsOf(Long deptId) {
        SysDept dept = deptMapper.selectById(deptId);
        return dept == null ? null : dept.getAncestors();
    }

    @Test
    void 新增子部门时祖级链等于父级链加父ID() {
        Long root = create("总部", 0L);
        Long child = create("研发部", root);

        assertThat(ancestorsOf(root)).isEqualTo("0");
        assertThat(ancestorsOf(child)).isEqualTo("0," + root);
    }

    @Test
    void 移动部门后所有子孙的祖级链一起刷新() {
        Long root = create("总部", 0L);
        Long a = create("A", root);
        Long b = create("B", a);
        Long c = create("C", b);

        // 把 B 挂到根下，C 的祖级链也应变成 0,root,B
        SysDeptForm move = new SysDeptForm();
        move.setDeptId(b);
        move.setDeptName("B");
        move.setParentId(root);
        deptService.update(move);

        assertThat(ancestorsOf(b)).isEqualTo("0," + root);
        assertThat(ancestorsOf(c)).isEqualTo("0," + root + "," + b);
        assertThat(ancestorsOf(a)).isEqualTo("0," + root);
    }

    @Test
    void 不能把上级部门设成自己的下级() {
        Long root = create("总部", 0L);
        Long child = create("子部门", root);

        SysDeptForm form = new SysDeptForm();
        form.setDeptId(root);
        form.setDeptName("总部");
        form.setParentId(child);

        assertThatThrownBy(() -> deptService.update(form))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("下级");
    }

    @Test
    void 上级部门不能是自己() {
        Long root = create("总部", 0L);

        SysDeptForm form = new SysDeptForm();
        form.setDeptId(root);
        form.setDeptName("总部");
        form.setParentId(root);

        assertThatThrownBy(() -> deptService.update(form))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能是自己");
    }

    @Test
    void 部门树按父子关系正确挂载() {
        Long root = create("总部", 0L);
        create("研发部", root);
        Long child = create("测试组", root);
        create("子分组", child);

        List<SysDeptVO> tree = deptService.tree(new SysDeptQuery());

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getDeptName()).isEqualTo("总部");
        assertThat(tree.get(0).getChildren()).hasSize(2);
    }

    @Test
    void 有子部门时不允许删除() {
        Long root = create("总部", 0L);
        create("研发部", root);

        assertThatThrownBy(() -> deptService.remove(root)).isInstanceOf(BizException.class);
    }
}
