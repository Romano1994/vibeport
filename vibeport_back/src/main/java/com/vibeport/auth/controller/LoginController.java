package com.vibeport.auth.controller;


import com.vibeport.auth.service.LoginService;
import com.vibeport.auth.vo.LoginResponse;
import com.vibeport.user.vo.UserVo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/vibeport/")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @PostMapping("login")
    public LoginResponse login(HttpServletRequest request, HttpServletResponse response, @RequestBody UserVo userVo) throws Exception{
        Map<String, Object> resultMap = this.loginService.login(userVo);

        // Authorization 헤더에 Bearer 접두어 추가
        response.setHeader("Authorization", "Bearer " + resultMap.get("access"));

        // ResponseCookie -> Set-Cookie 헤더로 추가 (ResponseCookie는 Cookie 객체가 아니므로 addCookie가 아님)
        response.addHeader("Set-Cookie", String.valueOf(resultMap.get("refresh")));

        return new LoginResponse((UserVo)resultMap.get("userVo"));
    }

    @PostMapping("logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception{
        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
