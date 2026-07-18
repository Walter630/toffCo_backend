package com.site.toffCo.infra.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

@Configuration
public class WebhookRedirectConfig {

    @Bean
    public FilterRegistrationBean<Filter> webhookRedirectFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                String uri = req.getRequestURI();

                if (uri.startsWith("/webhook/whatsapp/") && !uri.startsWith("/api/webhook/whatsapp/")) {
                    String newUri = "/api" + uri;
                    RequestDispatcher dispatcher = request.getRequestDispatcher(newUri);
                    dispatcher.forward(request, response);
                    return;
                }
                chain.doFilter(request, response);
            }
        });
        registration.addUrlPatterns("/webhook/whatsapp/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}