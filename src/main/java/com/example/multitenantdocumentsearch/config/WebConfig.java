package com.example.multitenantdocumentsearch.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the TenantInterceptor to enforce multi-tenancy on all API requests.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/static/**", "/public/**", "/resources/**", "/webjars/**",
                "/", "/index.html", "/favicon.ico",
                "/*.js", "/*.css", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg"
            );
    }
}
