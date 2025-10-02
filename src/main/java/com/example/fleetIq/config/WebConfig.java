package com.example.fleetIq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8080", "https://923cc1cddf51.ngrok-free.app/", "http://localhost:3000","https://s925b4gn-3000.brs.devtunnels.ms",
                        "https://vilox.pe.allmundotech.com","http://localhost:63342","https://geotrack.pe.allmundotech.com/")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}