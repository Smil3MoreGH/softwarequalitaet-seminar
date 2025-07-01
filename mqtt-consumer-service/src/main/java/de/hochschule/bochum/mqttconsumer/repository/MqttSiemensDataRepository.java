package de.hochschule.bochum.mqttconsumer.repository;

import de.hochschule.bochum.common.model.SiemensData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// Hier speichere und lese ich Siemens-S7-Daten aus MongoDB
@Repository
public interface MqttSiemensDataRepository extends MongoRepository<SiemensData, String> {
    // Gibt den neuesten Eintrag für einen bestimmten Typ (IST, SOLL, DIFFERENZ) zurück
    Optional<SiemensData> findTopByTypeOrderByTimestampDesc(String type);

    // Gibt alle Einträge für einen Typ zurück, sortiert nach Zeit (neueste zuerst)
    List<SiemensData> findByTypeOrderByTimestampDesc(String type);
}
