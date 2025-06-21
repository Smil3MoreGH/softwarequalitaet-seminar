package de.hochschule.bochum.restapi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final MongoTemplate mongoTemplate;

    @GetMapping
    public Map<String, Object> health() {
        log.info("GET /api/health aufgerufen");
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "REST API Service");

        try {
            long wagoCount = mongoTemplate.getCollection("wago_data").countDocuments();
            long siemensCount = mongoTemplate.getCollection("siemens_data").countDocuments();

            status.put("database", "CONNECTED");
            status.put("wagoDataCount", wagoCount);
            status.put("siemensDataCount", siemensCount);

            log.info("Health-Check: Datenbank CONNECTED, wagoDataCount={}, siemensDataCount={}", wagoCount, siemensCount);
        } catch (Exception e) {
            status.put("database", "DISCONNECTED");
            status.put("error", e.getMessage());
            log.error("Health-Check: Datenbank DISCONNECTED, Fehler: {}", e.getMessage());
        }

        return status;
    }
}
