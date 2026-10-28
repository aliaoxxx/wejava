package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.form.SysUserCreateForm;
import com.wejava.system.model.form.SysUserUpdateForm;
import com.wejava.system.model.query.SysUserQuery;
import com.wejava.system.model.vo.SysUserVO;
import java.util.List;

/**
 * 用户管理。
 */
public interface SysUserService {

    PageResult<SysUserVO> page(SysUserQuery query);

    /** 导出用：不翻页，最多 10000 条，同样受数据权限约束 */
    java.util.List<SysUserVO> exportList(SysUserQuery query);

    SysUserVO getById(Long userId);

    Long create(SysUserCreateForm form);

    void update(SysUserUpdateForm form);

    void remove(List<Long> userIds);

    void resetPassword(Long userId, String password);

    /** 修改自己的密码（校验原密码），成功后强制下线 */
    void changeOwnPassword(com.wejava.system.model.form.ProfilePasswordForm form);

    /** 修改自己的资料（昵称 / 手机 / 邮箱） */
    void updateOwnProfile(com.wejava.system.model.form.ProfileUpdateForm form);

    void changeStatus(Long userId, Integer status);
}
