package com.wejava.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wejava.system.model.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /** 部门下是否有用户（跨表，写在 XML） */
    int countUsersByDeptId(@Param("deptId") Long deptId);

    /**
     * 批量刷新子孙部门的祖级链。
     * <p>
     * 一条 SQL 用 CASE WHEN 更新多个不同的 ancestors 值，
     * 避免「每个节点一次 UPDATE」的 N+1。
     */
    @org.apache.ibatis.annotations.Update("<script>"
            + "UPDATE sys_dept SET ancestors = CASE dept_id "
            + "<foreach collection='list' item='dept'>WHEN #{dept.deptId} THEN #{dept.ancestors} </foreach>"
            + "END WHERE dept_id IN "
            + "<foreach collection='list' item='dept' open='(' separator=',' close=')'>#{dept.deptId}</foreach>"
            + "</script>")
    int updateAncestorsBatch(@Param("list") java.util.List<SysDept> depts);
}
