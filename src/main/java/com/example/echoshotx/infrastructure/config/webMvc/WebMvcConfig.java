package com.example.demo.infrastructure.config.webMvc;


import com.example.demo.infrastructure.security.resolver.CustomAuthenticationPrincipalArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String CORS_FRONT_PATH;
    private final CustomAuthenticationPrincipalArgumentResolver customAuthenticationPrincipalArgumentResolver;

    public WebMvcConfig(@Value("${app.cors.front-path}") String CORS_FRONT_PATH, CustomAuthenticationPrincipalArgumentResolver customAuthenticationPrincipalArgumentResolver) {
        this.CORS_FRONT_PATH = CORS_FRONT_PATH;
        this.customAuthenticationPrincipalArgumentResolver = customAuthenticationPrincipalArgumentResolver;
    }

    //resolver
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(customAuthenticationPrincipalArgumentResolver);
    }

    //CORS setting
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedOriginPatterns(CORS_FRONT_PATH)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

    }
}