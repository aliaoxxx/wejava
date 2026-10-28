package com.wejava.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wejava.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    @TableId(value = "dict_id", type = IdType.ASSIGN_ID)
    private Long dictId;

    private String dictName;

    private String dictType;

    private Integer status;

    private String remark;
}
