package de.hochschule.bochum.restapi.service;

import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.restapi.repository.SiemensDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SiemensService {

    private final SiemensDataRepository siemensRepository;

    public Optional<SiemensData> getLatestByType(String type) {
        return siemensRepository.findTopByTypeOrderByTimestampDesc(type);
    }

    public List<SiemensData> getAllByType(String type) {
        return siemensRepository.findByTypeOrderByTimestampDesc(type);
    }
}