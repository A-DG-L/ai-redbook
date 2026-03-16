package com.acorner.airedbook.config;

import com.acorner.airedbook.common.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns(   // 排除不需要登录的接口
                        "/api/user/register",
                        "/api/user/login",
                        "/error" // 排除 Spring Boot 默认错误处理路径
                );
    }
}