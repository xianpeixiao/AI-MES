package com.aimes.controller;

import com.aimes.common.Result;
import com.aimes.dto.request.auth.LoginRequest;
import com.aimes.dto.request.auth.PasswordChangeRequest;
import com.aimes.dto.request.auth.ProfileUpdateRequest;
import com.aimes.service.AuthService;
import com.aimes.vo.auth.AuthVo;
import com.aimes.vo.auth.CaptchaRequiredVo;
import com.aimes.vo.auth.CaptchaVo;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<AuthVo> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok("退出成功", null);
    }

    @GetMapping("/info")
    public Result<AuthVo> info() {
        return Result.ok(authService.info());
    }

    @GetMapping("/captcha")
    public Result<CaptchaVo> captcha() {
        return Result.ok(authService.captcha());
    }

    @GetMapping("/captcha/required")
    public Result<CaptchaRequiredVo> captchaRequired() {
        return Result.ok(authService.captchaRequired());
    }

    @PutMapping("/profile")
    public Result<AuthVo> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.ok(authService.updateProfile(request));
    }

    @PutMapping("/password")
    public Result<String> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request);
        return Result.ok("密码修改成功");
    }
}
