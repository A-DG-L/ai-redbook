package com.acorner.airedbook.service;

import com.acorner.airedbook.entity.Post;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface PostService extends IService<Post> {
    // 分页查询最新的帖子（按照创建时间倒序）
    Page<Post> getPostList(int pageNum, int pageSize);

    /**
     * 一键 AI 智能发帖（串联 OSS 与大模型）
     * @param userId 当前登录用户ID
     * @param file 上传的图片文件
     * @param prompt 用户的额外提示（可选）
     * @return 最终落库的帖子信息（包含生成的文案和图片URL）
     */
    Post publishWithAi(Long userId, MultipartFile file, String prompt);
}