package de.hochschule.bochum.restapi.controller;


import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.restapi.service.SiemensService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/siemens")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SiemensController {

    private final SiemensService siemensService;

    @GetMapping("/temperatur/ist/latest")
    public ResponseEntity<SiemensData> getLatestIstTemperatur() {
        return siemensService.getLatestByType("IST")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/temperatur/soll/latest")
    public ResponseEntity<SiemensData> getLatestSollTemperatur() {
        return siemensService.getLatestByType("SOLL")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/temperatur/differenz/latest")
    public ResponseEntity<SiemensData> getLatestDifferenzTemperatur() {
        return siemensService.getLatestByType("DIFFERENZ")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/temperatur/ist/all")
    public ResponseEntity<List<SiemensData>> getAllIstTemperatur() {
        return ResponseEntity.ok(siemensService.getAllByType("IST"));
    }

    @GetMapping("/temperatur/soll/all")
    public ResponseEntity<List<SiemensData>> getAllSollTemperatur() {
        return ResponseEntity.ok(siemensService.getAllByType("SOLL"));
    }

    @GetMapping("/temperatur/differenz/all")
    public ResponseEntity<List<SiemensData>> getAllDifferenzTemperatur() {
        return ResponseEntity.ok(siemensService.getAllByType("DIFFERENZ"));
    }
}