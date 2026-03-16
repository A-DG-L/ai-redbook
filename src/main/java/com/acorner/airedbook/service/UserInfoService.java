package com.acorner.airedbook.service;

import com.acorner.airedbook.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author adm
* @description 针对表【user_info(用户表)】的数据库操作Service
* @createDate 2026-03-10 16:39:47
*/
public interface UserInfoService extends IService<UserInfo> {
    String register(String username, String rawPassword);
    String login(String username, String rawPassword);

}
