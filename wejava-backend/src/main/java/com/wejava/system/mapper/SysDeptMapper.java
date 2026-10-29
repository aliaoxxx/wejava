package com.wejava.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wejava.system.model.entity.SysDept;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /** 部门下是否有用户（跨表，写在 XML） */
    int countUsersByDeptId(@Param("deptId") Long deptId);

    /**
     * 批量刷新子孙部门的祖级链（SQL 见 SysDeptMapper.xml）。
     * <p>
     * 一条 CASE WHEN 更新多个不同的 ancestors 值，避免「每个节点一次 UPDATE」的 N+1。
     * 返回 void：MySQL 的 UPDATE 报的是"变更行数"而非"匹配行数"，值本来就等于目标的行不计入，
     * 拿它做完整性校验会产生误报。
     */
    void updateAncestorsBatch(@Param("list") List<SysDept> depts);
}
