package com.jcarranza.minimidoffice.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web Application Context (child of the Root Context).
 *
 * Contains only Spring MVC beans: controllers, advice, HTTP configuration.
 * Inherits all beans from the Root Context (services, DAOs, etc.).
 * Loaded by DispatcherServlet (web.xml), which is mapped to /api/*.
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
    "com.jcarranza.minimidoffice.web.controller",
    "com.jcarranza.minimidoffice.web.advice"
})
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Jackson MVC — own ObjectMapper, independent of sabreObjectMapper.
     * Serializes LocalDate/LocalDateTime as ISO-8601 strings (not timestamps).
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converters.add(new MappingJackson2HttpMessageConverter(mapper));
    }

    /** Static resources for the frontend SPA (Phase 6). */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations("/static/");
    }

    /**
     * CORS: allows same-origin calls in local development.
     * In production, replace allowedOrigins with the real domain.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:8080", "http://localhost:8090")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }

    /**
     * Allows Tomcat DefaultServlet to serve index.html and static resources
     * without going through DispatcherServlet (which only handles /api/*).
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
}
