package com.acorner.airedbook.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 专项捕获我们自定义的业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        return Result.error(e.getMessage()); // 只给前端看友好的提示
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();
        return Result.error("服务器开小差了，请稍后再试");
    }
}