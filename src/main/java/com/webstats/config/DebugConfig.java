package com.webstats.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class DebugConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("\n=== REGISTERED REQUEST MAPPINGS ===");
        requestMappingHandlerMapping.getHandlerMethods().forEach((mapping, method) -> {
            System.out.println(mapping + " -> " + method);
        });
        System.out.println("=== END REQUEST MAPPINGS ===\n");
    }
}