package com.wejava.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wejava.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    private Long userId;

    private Long deptId;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String sex;

    private String password;

    private Integer status;

    @TableLogic
    private Integer delFlag;

    private LocalDateTime pwdUpdateTime;

    private String remark;
}
