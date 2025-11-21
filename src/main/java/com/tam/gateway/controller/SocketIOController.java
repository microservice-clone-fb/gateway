package com.tam.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tam.gateway.configuration.SocketIOService;
import com.tam.gateway.dto.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/internal/socketio")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SocketIOController {

    SocketIOService socketIOService;

    /**
     * Emit event to specific user via Socket.IO
     * Called by relationship-service to emit friend request events
     */
    @PostMapping("/emit")
    public ResponseEntity<ApiResponse<Void>> emitEvent(@RequestBody EmitEventRequest request) {
        log.info("📤 [Gateway] Received emit request - Event: {}, UserId: {}", request.getEventName(), request.getUserId());
        
        try {
            socketIOService.emitToUser(request.getUserId(), request.getEventName(), request.getData());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .code(1000)
                    .message("Event emitted successfully")
                    .build());
        } catch (Exception e) {
            log.error("❌ [Gateway] Failed to emit event", e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .code(5000)
                    .message("Failed to emit event: " + e.getMessage())
                    .build());
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EmitEventRequest {
        private String userId;
        private String eventName;
        private Object data;
    }
}

