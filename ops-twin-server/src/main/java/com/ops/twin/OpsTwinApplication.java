package com.ops.twin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@MapperScan("com.ops.twin.mapper")
@EnableAsync
public class OpsTwinApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpsTwinApplication.class, args);
    }
}
