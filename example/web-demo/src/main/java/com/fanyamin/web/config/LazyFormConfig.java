package com.fanyamin.web.config;

import com.fanyamin.LazyFormInstructor;
import com.fanyamin.instructor.llm.LlmClient;
import com.fanyamin.instructor.llm.LlmClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LazyFormConfig {

    @Bean
    public LlmClient llmClient() {
        return LlmClientFactory.createFromEnvironment();
    }

    @Bean
    public LazyFormInstructor lazyFormInstructor(LlmClient llmClient) {
        return new LazyFormInstructor(llmClient);
    }
}

