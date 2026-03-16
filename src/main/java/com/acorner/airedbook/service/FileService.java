package com.acorner.airedbook.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    /**
     * 上传文件到 OSS
     * @param file 前端传来的文件对象
     * @return 返回上传成功后的图片外网访问 URL
     */
    String uploadImage(MultipartFile file);

    /**
     * 上传服务器本地文件到 OSS (用于上传 FFmpeg 生成的视频)
     * @param localFile 本地文件对象
     * @param extension 文件后缀，如 ".mp4"
     * @return 返回上传成功后的公网访问 URL
     */
    String uploadLocalFile(java.io.File localFile, String extension);

    /**
     * 将公网 URL 的文件下载到服务器本地指定路径
     * @param fileUrl 公网文件 URL
     * @param targetPath 本地保存的绝对路径 (例如 F:\temp\img1.png)
     */
    void downloadUrlToFile(String fileUrl, String targetPath);
}