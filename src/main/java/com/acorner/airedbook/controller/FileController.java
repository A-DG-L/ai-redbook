package com.acorner.airedbook.controller;

import com.acorner.airedbook.common.Result; // 引入你 Day 1 封装的统一响应体
import com.acorner.airedbook.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 通用文件上传接口
     * * @param file 前端传来的文件
     * @return 返回文件的云端可访问 URL
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        log.info("接收到文件上传请求: {}", file.getOriginalFilename());

        // 校验文件是否为空
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空"); // 这里的 error 方法请根据你实际的 Result 类进行调整
        }

        // 调用 Service 上传并获取 URL
        String url = fileService.uploadImage(file);

        return Result.success(url);
    }
}