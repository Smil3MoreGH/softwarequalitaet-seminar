package de.hochschule.bochum.restapi.controller;

import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.restapi.service.SiemensService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/siemens")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SiemensController {

    private final SiemensService siemensService;

    @GetMapping("/temperatur/ist/latest")
    public ResponseEntity<SiemensData> getLatestIstTemperatur() {
        log.info("GET /api/siemens/temperatur/ist/latest aufgerufen");
        return siemensService.getLatestByType("IST")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/temperatur/soll/latest")
    public ResponseEntity<SiemensData> getLatestSollTemperatur() {
        log.info("GET /api/siemens/temperatur/soll/latest aufgerufen");
        return siemensService.getLatestByType("SOLL")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/temperatur/differenz/latest")
    public ResponseEntity<SiemensData> getLatestDifferenzTemperatur() {
        log.info("GET /api/siemens/temperatur/differenz/latest aufgerufen");
        return siemensService.getLatestByType("DIFFERENZ")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/temperatur/ist/all")
    public ResponseEntity<List<SiemensData>> getAllIstTemperatur() {
        log.info("GET /api/siemens/temperatur/ist/all aufgerufen");
        return ResponseEntity.ok(siemensService.getAllByType("IST"));
    }

    @GetMapping("/temperatur/soll/all")
    public ResponseEntity<List<SiemensData>> getAllSollTemperatur() {
        log.info("GET /api/siemens/temperatur/soll/all aufgerufen");
        return ResponseEntity.ok(siemensService.getAllByType("SOLL"));
    }

    @GetMapping("/temperatur/differenz/all")
    public ResponseEntity<List<SiemensData>> getAllDifferenzTemperatur() {
        log.info("GET /api/siemens/temperatur/differenz/all aufgerufen");
        return ResponseEntity.ok(siemensService.getAllByType("DIFFERENZ"));
    }
}