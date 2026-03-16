package com.acorner.airedbook;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.acorner.airedbook.mapper") // 👈 就是这一行！告诉 Spring Boot 去这个包下把所有的 Mapper 收编入伍
public class AiRedbookApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRedbookApplication.class, args);
    }
}