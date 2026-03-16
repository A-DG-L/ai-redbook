package com.acorner.airedbook.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostPublishDTO {
    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank(message = "媒体资源不能为空")
    private String mediaUrl;

    private String mediaType; // IMAGE 或 VIDEO
}