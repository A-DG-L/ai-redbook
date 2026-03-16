package com.acorner.airedbook.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;

public class JwtUtil {
    // 秘钥（生产环境应放入 application.yml 中）
    private static final String SECRET = "AiRedbook_Hackathon_Secret_2026";
    // 过期时间：7天
    private static final long EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;

    public static String createToken(Long userId) {
        return JWT.create()
                .withClaim("userId", userId)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .sign(Algorithm.HMAC256(SECRET));
    }

    public static Long getUserId(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(SECRET)).build().verify(token);
            return jwt.getClaim("userId").asLong();
        } catch (Exception e) {
            return null; // 解析失败或过期
        }
    }
}