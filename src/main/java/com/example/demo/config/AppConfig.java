package com.example.demo.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder.build();

        // 移除原本的 StringHttpMessageConverter，避免用預設 ISO-8859-1
        restTemplate.getMessageConverters().removeIf(
                converter -> converter instanceof StringHttpMessageConverter
        );

        // 改成 UTF-8 優先
        restTemplate.getMessageConverters().add(
                0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8)
        );

        return restTemplate;
    }
}
