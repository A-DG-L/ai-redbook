package com.acorner.airedbook.entity;

import com.acorner.airedbook.entity.Post;
import com.acorner.airedbook.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostLikeSyncTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final PostService postService;

    /**
     * 定时任务：每隔 5 分钟执行一次 (300000毫秒)
     * 把 Redis 中发生变动的帖子点赞数，同步更新到 MySQL 中
     */
    @Scheduled(fixedRate = 300000)
    public void syncPostLikesToDatabase() {
        log.info("⏳ 开始执行定时任务：将 Redis 点赞数同步到 MySQL...");

        // 1. 获取所有发生了点赞变动的 帖子ID 集合
        String modifiedSetKey = "sys:post:like:modified";
        Set<String> modifiedPostIds = stringRedisTemplate.opsForSet().members(modifiedSetKey);

        if (modifiedPostIds == null || modifiedPostIds.isEmpty()) {
            log.info("✅ 当前无变动的点赞数据，同步结束。");
            return;
        }

        log.info("👀 发现 {} 个帖子的点赞数发生变化，准备同步落盘...", modifiedPostIds.size());

        int successCount = 0;

        for (String postIdStr : modifiedPostIds) {
            try {
                // 2. 拼装当前帖子的总赞数 Key
                String countKey = "post:like:count:" + postIdStr;
                String countStr = stringRedisTemplate.opsForValue().get(countKey);

                if (countStr != null) {
                    int finalLikeCount = Integer.parseInt(countStr);
                    Long postId = Long.parseLong(postIdStr);

                    // 3. 组装实体，更新数据库里的 post 表
                    Post post = new Post();
                    post.setId(postId);
                    post.setLikeCount(finalLikeCount);

                    // 这里 Mybatis-Plus 会执行: UPDATE post SET like_count = ? WHERE id = ?
                    postService.updateById(post);

                    // 4. 落盘成功后，从“变动名单”中剔除这个帖子，避免下次重复同步
                    stringRedisTemplate.opsForSet().remove(modifiedSetKey, postIdStr);
                    successCount++;
                }
            } catch (Exception e) {
                // 如果某一个帖子同步失败，打印错误，但不中断循环，继续同步下一个
                log.error("❌ 同步帖子 {} 的点赞数到 MySQL 失败: {}", postIdStr, e.getMessage());
            }
        }

        log.info("🎉 定时同步任务完成！成功将 {} 个帖子的点赞数刷入数据库。", successCount);
    }
}
