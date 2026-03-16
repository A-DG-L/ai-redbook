package com.acorner.airedbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 点赞记录表
 * @TableName post_like
 */
@TableName(value ="post_like")
@Data
public class PostLike {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    private Long postId;

    /**
     * 
     */
    private Long userId;

    /**
     * 
     */
    private LocalDateTime createTime;
}