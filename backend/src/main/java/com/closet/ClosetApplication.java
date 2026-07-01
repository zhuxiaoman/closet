package com.closet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.closet.mapper")
public class ClosetApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClosetApplication.class, args);
    }
}
