package com.fanyamin.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class
})
public class SmartFormWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartFormWebApplication.class, args);
    }
}

