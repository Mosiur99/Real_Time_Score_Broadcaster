package com.scorebroadcaster.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration.
 *
 * <p>JSP view resolution is driven by {@code spring.mvc.view.prefix/suffix}
 * in application.properties.</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
}
