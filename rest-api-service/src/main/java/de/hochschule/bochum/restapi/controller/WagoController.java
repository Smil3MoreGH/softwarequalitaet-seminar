package de.hochschule.bochum.restapi.controller;

import de.hochschule.bochum.common.dto.ControlCommand;
import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.restapi.service.WagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/wago")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WagoController {

    private final WagoService wagoService;

    @GetMapping("/status/latest")
    public ResponseEntity<WagoData> getLatestStatus() {
        log.info("GET /api/wago/status/latest aufgerufen");
        return wagoService.getLatestStatus()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/status/latest/binary")
    public ResponseEntity<WagoData> getLatestStatusWithBinary() {
        log.info("GET /api/wago/status/latest/binary aufgerufen");
        return wagoService.getLatestStatus()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/control")
    public ResponseEntity<String> sendControlCommand(@RequestBody ControlCommand command) {
        log.info("POST /api/wago/control aufgerufen – Command: {}", command.getCommand());
        wagoService.sendControlCommand(command.getCommand());
        return ResponseEntity.ok("Command sent: " + command.getCommand());
    }
}
