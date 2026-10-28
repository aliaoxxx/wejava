package com.wejava.common.exception;

import lombok.Getter;

/**
 * 错误码。HTTP 状态码与业务码分离：HTTP 只表达协议层，业务码表达具体原因。
 */
@Getter
public enum ErrorCode {
    SUCCESS(200, "操作成功"),

    // 协议层
    BAD_REQUEST(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后重试"),
    INTERNAL_ERROR(500, "系统繁忙，请稍后重试"),

    // 认证 1xxx
    BAD_CREDENTIALS(1001, "用户名或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已被停用"),
    ACCOUNT_LOCKED(1003, "账号已锁定，请稍后再试"),
    CAPTCHA_ERROR(1004, "验证码错误或已过期"),
    TOKEN_EXPIRED(1005, "登录已过期"),
    NOT_LOGIN(1006, "登录状态已失效，请重新登录"),
    TOKEN_INVALID(1006, "登录状态已失效，请重新登录"),
    OLD_PASSWORD_ERROR(1007, "原密码错误"),
    PASSWORD_NOT_MATCH(1008, "两次输入的密码不一致"),
    PASSWORD_SAME(1009, "新密码不能与原密码相同"),

    // 业务 2xxx
    DATA_NOT_FOUND(2001, "数据不存在"),
    DATA_ALREADY_EXISTS(2002, "数据已存在"),
    HAS_CHILDREN(2003, "存在子节点，无法删除"),
    HAS_REFERENCE(2004, "存在关联数据，无法删除"),
    CANNOT_OPERATE_ADMIN(2005, "不允许操作超级管理员"),
    CANNOT_OPERATE_SELF(2006, "不允许操作当前登录用户"),
    DEPT_HAS_USER(2007, "部门下存在用户，无法删除"),
    ROLE_HAS_USER(2008, "角色已分配给用户，无法删除"),

    // 文件 3xxx
    UPLOAD_FAILED(3001, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(3002, "文件类型不允许"),
    FILE_TOO_LARGE(3003, "文件大小超出限制"),
    EXPORT_TOO_LARGE(3004, "导出数据量过大，请缩小范围"),
    FILE_NOT_FOUND(3005, "文件不存在或已过期");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
