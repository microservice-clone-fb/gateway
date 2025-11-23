package com.tam.gateway.configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Slf4j
public class SocketIOService implements ApplicationListener<ApplicationReadyEvent> {

    final SocketIOServer socketIOServer;
    volatile boolean started = false;

    /**
     * Start Socket.IO server AFTER Spring Cloud Gateway is ready
     * This avoids port conflict by starting Socket.IO server after Gateway has bound to the port
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!started) {
            start();
        }
    }

    /**
     * Fallback: Start immediately if ApplicationReadyEvent doesn't fire
     */
    @PostConstruct
    public void init() {
        // Don't start here - wait for ApplicationReadyEvent
        // This ensures Spring Cloud Gateway starts first
    }

    private void start() {
        if (started) {
            return;
        }

        try {
            log.info("🚀 Starting Socket.IO server in Gateway...");
            socketIOServer.addConnectListener(onConnected());
            socketIOServer.addDisconnectListener(onDisconnected());
            socketIOServer.start();
            started = true;
            log.info("✅ Socket.IO server started successfully in Gateway");
            log.info("📝 Socket.IO server listening on 0.0.0.0:9092 - ready to accept connections");
            log.info("📝 Waiting for client connections...");
        } catch (Exception e) {
            log.error("❌ Failed to start Socket.IO server in Gateway", e);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("🛑 Stopping Socket.IO server in Gateway...");
        socketIOServer.stop();
        log.info("✅ Socket.IO server stopped in Gateway");
    }

    private ConnectListener onConnected() {
        return socket -> {
            try {
                String userId = socket.getHandshakeData().getSingleUrlParam("userId");
                String token = socket.getHandshakeData().getSingleUrlParam("token");
                String remoteAddress = socket.getRemoteAddress().toString();
                
                log.info("✅ Socket client connected - Session ID: {}, User ID: {}, Remote: {}", 
                        socket.getSessionId(), userId, remoteAddress);
                log.debug("📝 Connection details - Token present: {}, Headers: {}", 
                        token != null, socket.getHandshakeData().getHttpHeaders());

                // Store user session if needed
                socket.set("userId", userId);
            } catch (Exception e) {
                log.error("❌ Error in onConnected handler", e);
            }
        };
    }

    private DisconnectListener onDisconnected() {
        return socket -> {
            try {
                String userId = (String) socket.get("userId");
                log.info("❌ Socket client disconnected - Session ID: {}, User ID: {}", 
                        socket.getSessionId(), userId);
            } catch (Exception e) {
                log.error("❌ Error in onDisconnected handler", e);
            }
        };
    }


    /**
     * Emit event to specific user by userId
     */
    public void emitToUser(String userId, String eventName, Object data) {
        log.info("🔍 [SocketIO] Looking for connected client with userId: {}", userId);
        int connectedClients = socketIOServer.getAllClients().size();
        log.info("🔍 [SocketIO] Total connected clients: {}", connectedClients);

        final boolean[] found = {false};
        socketIOServer.getAllClients().forEach(client -> {
            String clientUserId = (String) client.get("userId");
            log.debug("🔍 [SocketIO] Checking client - Session: {}, UserId: {}", client.getSessionId(), clientUserId);

            if (userId.equals(clientUserId)) {
                log.info("✅ [SocketIO] Found matching client! Emitting event '{}' to user: {}", eventName, userId);
                client.sendEvent(eventName, data);
                log.info("📤 [SocketIO] Event '{}' emitted successfully to user: {}", eventName, userId);
                found[0] = true;
            }
        });

        if (!found[0]) {
            log.warn("⚠️ [SocketIO] User {} is not connected. Event '{}' will not be delivered.", userId, eventName);
            log.warn("⚠️ [SocketIO] Make sure user {} has Socket.IO connection active.", userId);
        }
    }

    /**
     * Emit event to all connected clients
     */
    public void emitToAll(String eventName, Object data) {
        socketIOServer.getBroadcastOperations().sendEvent(eventName, data);
        log.debug("📢 Broadcasted event '{}' to all clients", eventName);
    }
}

