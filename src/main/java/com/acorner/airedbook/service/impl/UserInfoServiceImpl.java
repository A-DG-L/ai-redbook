package com.acorner.airedbook.service.impl;

import com.acorner.airedbook.common.utils.JwtUtil;
import com.acorner.airedbook.entity.UserInfo;
import com.acorner.airedbook.mapper.UserInfoMapper;
import com.acorner.airedbook.service.UserInfoService;
import com.acorner.airedbook.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    public String register(String username, String rawPassword) {
        // 1. 检查用户名是否存在
        long count = this.count(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUsername, username));
        if (count > 0) {
            throw new RuntimeException("用户名已存在"); // 这里后续可以换成你的自定义业务异常
        }

        // 2. 密码加密 (面试亮点：绝对不在数据库存明文)
        String hashedPwd = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        // 3. 落库
        UserInfo user = new UserInfo();
        user.setUsername(username);
        user.setPassword(hashedPwd);
        this.save(user);

        return "注册成功";
    }

    public String login(String username, String rawPassword) {
        // 1. 根据用户名查询用户
        UserInfo user = this.getOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUsername, username));
        if (user == null) {
            throw new RuntimeException("用户不存在或密码错误");
        }

        // 2. 校验 BCrypt 密码
        if (!BCrypt.checkpw(rawPassword, user.getPassword())) {
            throw new RuntimeException("用户不存在或密码错误");
        }

        // 3. 生成并返回 JWT Token
        return JwtUtil.createToken(user.getId());
    }
}