package com.fanyamin.workflow.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration.class
})
public class WorkflowWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowWebApplication.class, args);
    }
}

