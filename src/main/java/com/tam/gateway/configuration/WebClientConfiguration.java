package com.tam.gateway.configuration;

import com.tam.gateway.repository.IdentityClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebClientConfiguration {
    @Value("${app.identity-service.url}")
    String urlIdentity;

    @Value("${app.allowed-origins}")
    private String allowedOrigins;

    @Bean
    WebClient webClient() {
        return WebClient.builder()
                .baseUrl(urlIdentity)
                .build();
    }

    /**
     * CORS Configuration for Spring Cloud Gateway
     * ⚠️ QUAN TRỌNG: Phải dùng addAllowedOriginPattern() thay vì addAllowedOrigin()
     * khi có allowCredentials = true
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // Chạy trước AuthenticationFilter
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // ✅ Parse và add origins - hỗ trợ cả exact match và patterns
        String[] origins = allowedOrigins.split(",");
        for (String origin : origins) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                // ✅ Dùng addAllowedOriginPattern để hỗ trợ wildcards
                // Ví dụ: https://*.ngrok-free.app
                corsConfig.addAllowedOriginPattern(trimmed);
            }
        }

        // ✅ Allow all common HTTP methods
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // ✅ Allow all headers
        corsConfig.addAllowedHeader("*");

        // ✅ Expose headers để frontend đọc được
        corsConfig.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count",
                "X-Request-Id",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // ✅ QUAN TRỌNG: Cho phép gửi credentials (cookies, auth headers)
        corsConfig.setAllowCredentials(true);

        // ✅ Cache preflight requests for 1 hour
        corsConfig.setMaxAge(3600L);

        // ✅ Apply to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    @Bean
    IdentityClient identityClient(WebClient webClient) {
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build();

        return httpServiceProxyFactory.createClient(IdentityClient.class);
    }
}