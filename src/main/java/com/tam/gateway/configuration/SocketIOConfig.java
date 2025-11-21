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
        
        // Thêm config cho React Native client
        config.setPingTimeout(60000); // 60 seconds
        config.setPingInterval(25000); // 25 seconds
        config.setUpgradeTimeout(10000); // 10 seconds
        config.setMaxHttpContentLength(1048576); // 1MB
        
        // Enable both websocket and polling transports
        config.setTransports(com.corundumstudio.socketio.Transport.WEBSOCKET, com.corundumstudio.socketio.Transport.POLLING);
        
        // Additional config for better compatibility
        config.setRandomSession(true); // Generate random session IDs
        // Không set AuthorizationListener - cho phép tất cả connections
        // Authentication sẽ được handle trong ConnectListener

        SocketIOServer server = new SocketIOServer(config);
        log.info("🔌 Socket.IO server will start on {}:{} (separate port from HTTP server)", host, port);
        log.info("⚠️ Note: netty-socketio runs as standalone server, cannot share port with Spring Cloud Gateway");
        log.info("📝 Socket.IO config: pingTimeout={}ms, pingInterval={}ms, upgradeTimeout={}ms", 
                config.getPingTimeout(), config.getPingInterval(), config.getUpgradeTimeout());
        return server;
    }
}

