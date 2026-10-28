package com.wejava.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wejava.system.model.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /** 部门下是否有用户（跨表，写在 XML） */
    int countUsersByDeptId(@Param("deptId") Long deptId);
}
