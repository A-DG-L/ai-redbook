package com.acorner.airedbook.controller;

import com.acorner.airedbook.common.Result;
import com.acorner.airedbook.entity.dto.UserAuthDTO;
import com.acorner.airedbook.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 用户注册接口
     * @RequestBody 表示接收前端传来的 JSON 数据
     * @Validated 配合 DTO 里的 @NotBlank 开启参数校验
     */
    @PostMapping("/register")
    public Result<String> register(@Validated @RequestBody UserAuthDTO authDTO) {
        String msg = userInfoService.register(authDTO.getUsername(), authDTO.getPassword());
        return Result.success(msg);
    }

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    public Result<String> login(@Validated @RequestBody UserAuthDTO authDTO) {
        // 登录成功会返回 JWT Token
        String token = userInfoService.login(authDTO.getUsername(), authDTO.getPassword());
        return Result.success(token);
    }
}