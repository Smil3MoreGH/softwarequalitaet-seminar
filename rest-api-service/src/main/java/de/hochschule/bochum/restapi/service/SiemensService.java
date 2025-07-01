package de.hochschule.bochum.restapi.service;

import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.restapi.repository.SiemensDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

// Service-Klasse, die die Siemens-Datenbankabfragen kapselt (wird im Controller verwendet)
@Service
@RequiredArgsConstructor
public class SiemensService {

    private final SiemensDataRepository siemensRepository;

    // Holt den aktuellsten Eintrag eines bestimmten Typs
    public Optional<SiemensData> getLatestByType(String type) {
        return siemensRepository.findTopByTypeOrderByTimestampDesc(type);
    }

    // Holt alle Einträge eines bestimmten Typs
    public List<SiemensData> getAllByType(String type) {
        return siemensRepository.findByTypeOrderByTimestampDesc(type);
    }
}
