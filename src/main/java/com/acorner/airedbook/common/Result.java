package com.acorner.airedbook.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 200代表成功，500代表失败
    private String msg;   // 提示信息
    private T data;       // 真正要返回的数据

    // 成功时的快捷方法
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    // 失败时的快捷方法
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }
}