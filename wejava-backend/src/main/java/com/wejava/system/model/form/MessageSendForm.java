package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class MessageSendForm {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100")
    private String title;

    @Size(max = 5000, message = "内容最长 5000")
    private String content;

    /** 1 公告 2 系统 3 个人 */
    private Integer msgType;

    /** 0 普通 1 重要 2 紧急 */
    private Integer level;

    /** 点击跳转的前端路由 */
    @Size(max = 200, message = "跳转地址最长 200")
    private String link;

    /** 1 全员 2 按角色 3 按部门 4 指定用户 */
    @NotNull(message = "请选择接收范围")
    private Integer targetType;

    /** targetType 为 2/3/4 时必填 */
    private List<Long> targetIds;
}
