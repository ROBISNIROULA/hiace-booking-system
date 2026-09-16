package com.hiace.hiacebooking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded images from static/uploads/
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations( "file:" +
                        System.getProperty("user.dir") +
                        "/src/main/resources/static/uploads/");
    }
}
