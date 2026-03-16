package com.acorner.airedbook.service.impl;

import com.acorner.airedbook.entity.Post;
import com.acorner.airedbook.entity.dto.AiGenerationResult; // 🌟 引入 DTO
import com.acorner.airedbook.mapper.PostMapper;
import com.acorner.airedbook.service.AiService;
import com.acorner.airedbook.service.FileService;
import com.acorner.airedbook.service.PostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final FileService fileService;
    private final AiService aiService;

    @Override
    public Page<Post> getPostList(int pageNum, int pageSize) {
        Page<Post> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .orderByDesc(Post::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Post publishWithAi(Long userId, MultipartFile file, String prompt) {
        log.info("用户 {} 开始执行一键 AI 发帖", userId);

        String imageUrl = fileService.uploadImage(file);
        log.info("图片已上传 OSS: {}", imageUrl);

        // 🌟 核心修改 1：由于这是一键发帖（不走草稿箱），我们生成一个临时的时间戳作为 taskId 传入，满足接口签名
        Long tempTaskId = System.currentTimeMillis();

        // 🌟 核心修改 2：把 tempTaskId 放在第一个参数的位置！
        AiGenerationResult aiResult = aiService.generateRedbookCopywriting(tempTaskId, userId, imageUrl, prompt);

        String generatedContent = aiResult.getContent(); // 提取文案
        log.info("AI 文案生成完毕");

        Post post = new Post();
        post.setUserId(userId);
        post.setContent(generatedContent); // 塞入文案
        post.setMediaUrl(imageUrl);
        post.setMediaType(1); // 1: 图文
        post.setIsAiGenerated(1);
        post.setLikeCount(0);
        post.setCreateTime(LocalDateTime.now());

        this.save(post);
        log.info("AI 帖子已成功存入数据库，帖子ID: {}", post.getId());

        return post;
    }
}