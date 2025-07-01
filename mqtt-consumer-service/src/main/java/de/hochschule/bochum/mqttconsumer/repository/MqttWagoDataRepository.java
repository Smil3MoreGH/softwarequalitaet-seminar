package de.hochschule.bochum.mqttconsumer.repository;

import de.hochschule.bochum.common.model.WagoData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// Hier speichere und lese ich Wago-SPS-Daten aus MongoDB
@Repository
public interface MqttWagoDataRepository extends MongoRepository<WagoData, String> {
    // Gibt den neuesten Status-Eintrag zurück (z.B. für das Frontend)
    Optional<WagoData> findTopByOrderByTimestampDesc();
}
