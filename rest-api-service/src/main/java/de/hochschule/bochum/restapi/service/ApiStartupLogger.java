package de.hochschule.bochum.restapi.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Loggt beim Start, dass mein REST-API-Service erfolgreich läuft
@Slf4j
@Component
public class ApiStartupLogger {
    @PostConstruct
    public void logStartup() {
        log.info("REST-API-Service wurde erfolgreich gestartet!");
    }
}
