package com.webstats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.webstats")
public class WebStatsApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebStatsApplication.class, args);
    }
}