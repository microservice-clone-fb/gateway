package com.tam.gateway.configuration;

import com.tam.gateway.repository.IdentityClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

@Configuration
public class WebClientConfiguration {
    @Value("${app.identity-service.url}")
    String urlIdentity;

    @Value("${app.allowed-origins}")
    private String allowedOrigins;
    @Bean
    WebClient webClient(){
        return WebClient.builder()
                .baseUrl(urlIdentity)
                .build();
    }

    @Bean
    CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        // Parse multiple origins từ config (format: "origin1,origin2,origin3")
        List<String> originsList = List.of(allowedOrigins.split(","));
        corsConfig.setAllowedOrigins(originsList);
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        // Tạo filter với order thấp hơn AuthenticationFilter để chạy trước
        CorsWebFilter filter = new CorsWebFilter(source);
        return filter;
    }


    @Bean
    IdentityClient identityClient(WebClient webClient){
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient)).build();

        return httpServiceProxyFactory.createClient(IdentityClient.class);
    }

}
