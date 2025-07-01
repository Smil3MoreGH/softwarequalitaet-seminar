package de.hochschule.bochum.restapi.repository;

import de.hochschule.bochum.common.model.WagoData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// Repository für Wago-Daten – holt immer den aktuellsten Status für die Lampen
@Repository
public interface WagoDataRepository extends MongoRepository<WagoData, String> {
    Optional<WagoData> findTopByOrderByTimestampDesc();
}
