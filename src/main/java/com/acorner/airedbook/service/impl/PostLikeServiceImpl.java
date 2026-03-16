package com.acorner.airedbook.service.impl;

import com.acorner.airedbook.entity.PostLike;
import com.acorner.airedbook.mapper.PostLikeMapper;
import com.acorner.airedbook.service.PostLikeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl extends ServiceImpl<PostLikeMapper, PostLike> implements PostLikeService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean toggleLike(Long postId, Long userId) {
        // 1. 定义 Redis 的 Key
        // 用于记录哪些用户点赞了这个帖子 (Set 结构)
        String userSetKey = "post:like:users:" + postId;
        // 用于记录帖子的总点赞数 (String 结构)
        String countKey = "post:like:count:" + postId;
        // 用于记录哪些帖子的点赞数发生过变动 (为后续定时任务做准备)
        String modifiedSetKey = "sys:post:like:modified";

        // 2. 判断用户是否已经点赞过
        Boolean isLiked = stringRedisTemplate.opsForSet().isMember(userSetKey, userId.toString());

        if (Boolean.TRUE.equals(isLiked)) {
            // ========================
            // 场景 A：已经点过赞 -> 执行取消点赞
            // ========================
            stringRedisTemplate.opsForSet().remove(userSetKey, userId.toString()); // 从 Redis 集合移除
            stringRedisTemplate.opsForValue().decrement(countKey); // Redis 总数 - 1
            stringRedisTemplate.opsForSet().add(modifiedSetKey, postId.toString()); // 标记该帖子有变动

            // 从 MySQL 的 post_like 表删除记录
            LambdaQueryWrapper<PostLike> query = new LambdaQueryWrapper<>();
            query.eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId);
            this.remove(query);

            log.info("用户 {} 取消了对帖子 {} 的点赞", userId, postId);
            return false;
        } else {
            // ========================
            // 场景 B：尚未点赞 -> 执行点赞
            // ========================
            stringRedisTemplate.opsForSet().add(userSetKey, userId.toString()); // 加入 Redis 集合
            stringRedisTemplate.opsForValue().increment(countKey); // Redis 总数 + 1
            stringRedisTemplate.opsForSet().add(modifiedSetKey, postId.toString()); // 标记该帖子有变动

            // 写入 MySQL 的 post_like 表
            PostLike postLike = new PostLike();
            postLike.setPostId(postId);
            postLike.setUserId(userId);
            postLike.setCreateTime(LocalDateTime.now());
            this.save(postLike);

            log.info("用户 {} 点赞了帖子 {}", userId, postId);
            return true;
        }
    }
}