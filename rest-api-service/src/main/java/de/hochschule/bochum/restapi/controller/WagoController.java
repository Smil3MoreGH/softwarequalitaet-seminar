package de.hochschule.bochum.restapi.controller;

import de.hochschule.bochum.common.dto.ControlCommand;
import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.restapi.service.WagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wago")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WagoController {

    private final WagoService wagoService;

    @GetMapping("/status/latest")
    public ResponseEntity<WagoData> getLatestStatus() {
        return wagoService.getLatestStatus()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/status/latest/binary")
    public ResponseEntity<WagoData> getLatestStatusWithBinary() {
        return wagoService.getLatestStatus()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/control")
    public ResponseEntity<String> sendControlCommand(@RequestBody ControlCommand command) {
        wagoService.sendControlCommand(command.getCommand());
        return ResponseEntity.ok("Command sent: " + command.getCommand());
    }
}