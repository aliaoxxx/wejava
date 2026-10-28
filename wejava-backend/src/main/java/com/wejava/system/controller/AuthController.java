package com.wejava.system.controller;

import com.wejava.common.annotation.RateLimit;
import com.wejava.common.core.R;
import com.wejava.common.web.IpUtils;
import com.wejava.security.model.AuthUser;
import com.wejava.security.model.Captcha;
import com.wejava.security.service.CaptchaService;
import com.wejava.system.converter.MeConverter;
import com.wejava.system.model.form.LoginForm;
import com.wejava.system.model.form.RefreshForm;
import com.wejava.system.model.vo.LoginVO;
import com.wejava.system.model.vo.MeVO;
import com.wejava.system.model.vo.RouterVO;
import com.wejava.system.service.LoginService;
import com.wejava.system.service.SysMenuService;
import com.wejava.system.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final TokenService tokenService;
    private final CaptchaService captchaService;
    private final MeConverter meConverter;
    private final SysMenuService menuService;

    @RateLimit(count = 5, seconds = 10, message = "验证码请求过于频繁，请稍后再试")
    @GetMapping("/captcha")
    public R<Captcha> captcha() {
        return R.ok(captchaService.generate());
    }

    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody @Valid LoginForm form, HttpServletRequest request) {
        String ip = IpUtils.getIp(request);
        String userAgent = request.getHeader("User-Agent");
        return R.ok(loginService.login(form, ip, userAgent));
    }

    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestBody @Valid RefreshForm form) {
        return R.ok(tokenService.refresh(form.getRefreshToken()));
    }

    @PostMapping("/logout")
    public R<Void> logout(@AuthenticationPrincipal AuthUser authUser) {
        tokenService.logout(authUser == null ? null : authUser.getSession());
        return R.ok();
    }

    @GetMapping("/me")
    public R<MeVO> me(@AuthenticationPrincipal AuthUser authUser) {
        return R.ok(meConverter.toVO(authUser.getSession()));
    }

    /** 当前用户可见的动态路由树 */
    @GetMapping("/routers")
    public R<List<RouterVO>> routers(@AuthenticationPrincipal AuthUser authUser) {
        return R.ok(menuService.routers(authUser.getSession()));
    }
}
