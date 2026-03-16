package com.acorner.airedbook.service.impl;

import com.acorner.airedbook.common.BusinessException;
import com.aliyun.oss.OSS;
import com.acorner.airedbook.config.OssProperties;
import com.acorner.airedbook.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final OSS ossClient;
    private final OssProperties ossProperties;

    @Override
    public String uploadImage(MultipartFile file) {
        try {
            // 1. 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            // 2. 提取文件后缀 (例如: .jpg, .png)
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 3. 使用 UUID 生成新的唯一文件名，按日期分目录更好，这里 MVP 版本为了快直接丢根目录
            String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

            // 4. 获取文件输入流
            InputStream inputStream = file.getInputStream();

            // 5. 执行上传请求到 OSS
            ossClient.putObject(ossProperties.getBucketName(), newFileName, inputStream);

            // 6. 拼接最终的访问 URL
            // 格式：https://{bucketName}.{endpoint}/{newFileName}
            // 注意：endpoint 开头可能有 https:// 或没有，需要处理一下
            String endpoint = ossProperties.getEndpoint();
            if (endpoint.startsWith("https://")) {
                endpoint = endpoint.substring(8);
            } else if (endpoint.startsWith("http://")) {
                endpoint = endpoint.substring(7);
            }

            String url = "https://" + ossProperties.getBucketName() + "." + endpoint + "/" + newFileName;
            log.info("文件上传成功，访问路径: {}", url);

            return url;

        } catch (Exception e) {
            log.error("文件上传到 OSS 失败", e);
            throw new BusinessException("图片上传失败，请检查网络或配置");
        }
    }
    @Override
    public String uploadLocalFile(java.io.File localFile, String extension) {
        try {
            String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
            // 直接用 FileInputStream 读取本地硬盘的文件上传
            ossClient.putObject(ossProperties.getBucketName(), newFileName, new java.io.FileInputStream(localFile));

            String endpoint = ossProperties.getEndpoint();
            if (endpoint.startsWith("https://")) {
                endpoint = endpoint.substring(8);
            } else if (endpoint.startsWith("http://")) {
                endpoint = endpoint.substring(7);
            }
            String url = "https://" + ossProperties.getBucketName() + "." + endpoint + "/" + newFileName;
            log.info("本地文件上传 OSS 成功，访问路径: {}", url);
            return url;
        } catch (Exception e) {
            log.error("本地文件上传 OSS 失败", e);
            throw new BusinessException("文件上传云端失败");
        }
    }

    @Override
    public void downloadUrlToFile(String fileUrl, String targetPath) {
        try {
            // Java 现代写法：直接将 URL 输入流拷贝到本地文件路径，如果存在就覆盖
            java.nio.file.Files.copy(
                    java.net.URI.create(fileUrl).toURL().openStream(),
                    java.nio.file.Paths.get(targetPath),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            log.debug("成功下载文件: {} -> {}", fileUrl, targetPath);
        } catch (Exception e) {
            log.error("下载网络文件失败: {}", fileUrl, e);
            throw new BusinessException("拉取云端素材失败");
        }
    }
}