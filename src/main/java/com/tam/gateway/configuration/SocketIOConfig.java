package com.tam.gateway.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketIOServer;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class SocketIOConfig {

    @Value("${socketio.host:0.0.0.0}")
    private String host;

    // Socket.IO cần port riêng (netty-socketio là standalone Netty server)
    // Không thể bind cùng port với Spring Cloud Gateway server
    @Value("${socketio.port:9092}")
    private Integer port;

    @Bean
    public SocketIOServer socketIOServer() {
        // Use fully qualified name to avoid conflict with Spring's Configuration
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setAllowCustomRequests(true);
        config.setOrigin("*"); // Allow all origins in development (configure properly in production)

        SocketIOServer server = new SocketIOServer(config);
        log.info("🔌 Socket.IO server will start on {}:{} (separate port from HTTP server)", host, port);
        log.info("⚠️ Note: netty-socketio runs as standalone server, cannot share port with Spring Cloud Gateway");
        return server;
    }
}

