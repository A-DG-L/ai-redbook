package com.acorner.airedbook.service;

import com.acorner.airedbook.entity.PostLike;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PostLikeService extends IService<PostLike> {

    /**
     * 点赞或取消点赞 (高并发 Redis 版)
     * @param postId 帖子 ID
     * @param userId 用户 ID
     * @return true 表示点赞成功，false 表示取消点赞
     */
    boolean toggleLike(Long postId, Long userId);
}