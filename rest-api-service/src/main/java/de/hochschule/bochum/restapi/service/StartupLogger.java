package de.hochschule.bochum.restapi.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupLogger {
    @PostConstruct
    public void logStartup() {
        log.info("REST-API-Service wurde erfolgreich gestartet!");
    }
}