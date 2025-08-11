package com.sysml.platform.ui.sirius;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.Map;

/**
 * RQ-UI-RUNTIME-001: Sirius API控制器
 * 提供Sirius运行时状态查询接口
 */
@RestController
@RequestMapping("/api/sirius")
public class SiriusController {
    
    @Autowired
    private SiriusRuntime siriusRuntime;
    
    @Value("${server.port:8080}")
    private int serverPort;
    
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", siriusRuntime.isRunning());
        status.put("emfConnected", siriusRuntime.isEMFConnected());
        status.put("modelsLoaded", siriusRuntime.areModelsLoaded());
        status.put("version", siriusRuntime.getVersion());
        status.put("sessions", siriusRuntime.getActiveSessions());
        return status;
    }
    
    @GetMapping("/websocket/info")
    public Map<String, Object> getWebSocketInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("endpoint", "ws://localhost:" + serverPort + "/ws/sirius");
        info.put("protocol", "sirius-web");
        info.put("version", "1.0");
        return info;
    }
    
    @PostMapping("/session")
    public Map<String, Object> createSession() {
        String sessionId = "session-" + System.currentTimeMillis();
        siriusRuntime.createSession(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("status", "created");
        return response;
    }
    
    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> closeSession(@PathVariable String sessionId) {
        siriusRuntime.closeSession(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("status", "closed");
        return response;
    }
}