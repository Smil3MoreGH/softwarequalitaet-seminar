package de.hochschule.bochum.mqttconsumer.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Loggt beim Start einmal, dass mein Microservice läuft
@Slf4j
@Component
public class StartupLogger {
    @PostConstruct
    public void logStartup() {
        log.info("MQTT-Consumer Microservice wurde erfolgreich gestartet!");
    }
}
