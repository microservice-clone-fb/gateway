package com.tam.gateway.service;

import com.tam.gateway.dto.ApiResponse;
import com.tam.gateway.dto.request.IntrospectRequest;
import com.tam.gateway.dto.response.IntrospectResponse;
import com.tam.gateway.repository.IdentityClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class IdentityService {
    IdentityClient identityClient;

    public Mono<ApiResponse<IntrospectResponse>> introspect(String token){
        log.info("Run into identityService:::");
        return identityClient.introspect(IntrospectRequest.builder()
                        .token(token)
                .build())
                .doOnSuccess(response -> log.info("Introspect successful: {}", response))
                .doOnError(throwable -> log.error("Introspect failed: ", throwable));
    }
}
