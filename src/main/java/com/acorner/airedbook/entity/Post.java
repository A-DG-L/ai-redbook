package com.acorner.airedbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 帖子表
 * @TableName post
 */
@TableName(value ="post")
@Data
public class Post {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发布者ID
     */
    private Long userId;

    /**
     * 文案内容
     */
    private String content;

    /**
     * 视频/图片URL
     */
    private String mediaUrl;

    /**
     * 1:图文 2:视频
     */
    private Integer mediaType;

    /**
     * 是否为AI生成
     */
    private Integer isAiGenerated;

    /**
     * 点赞数(后期用Redis优化)
     */
    private Integer likeCount;

    /**
     * 
     */
    private LocalDateTime createTime;

    /**
     * 
     */
    private LocalDateTime updateTime;
}