package com.ecommerce.api.config;

import com.ecommerce.api.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {
}
