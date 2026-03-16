package com.acorner.airedbook.common.interceptor;

import com.acorner.airedbook.common.context.UserContext;
import com.acorner.airedbook.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头获取 Token (前端通常格式为 Authorization: Bearer <token>)
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 2. 校验 Token
        Long userId = JwtUtil.getUserId(token);
        if (userId == null) {
            // Token 无效或未登录，直接抛出异常，交由你的 GlobalExceptionHandler 处理
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\": 401, \"msg\": \"未登录或Token已过期\"}");
            return false;
        }

        // 3. 校验通过，存入 ThreadLocal
        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束，务必清理 ThreadLocal，防止内存泄漏
        UserContext.remove();
    }
}