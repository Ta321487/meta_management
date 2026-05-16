package ${packageName}.controller;

import ${packageName}.common.LoginRequest;
import ${packageName}.common.Result;
import ${packageName}.service.AuthService;
<#if captchaEnabled>
import ${packageName}.service.CaptchaService;
</#if>
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * 认证控制器（Session 登录<#if captchaEnabled> + 验证码</#if>）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

<#if captchaEnabled>
    @Autowired
    private CaptchaService captchaService;

    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.success(captchaService.createCaptcha());
    }
</#if>

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
<#if captchaEnabled>
        if (!captchaService.verifyAndConsume(loginRequest.getCaptchaKey(), loginRequest.getCaptchaCode())) {
            return Result.error("验证码错误或已过期");
        }
</#if>
        Map<String, Object> user = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        return Result.success(user);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.success();
    }
}
