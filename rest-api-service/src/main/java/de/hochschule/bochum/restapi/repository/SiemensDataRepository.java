package de.hochschule.bochum.restapi.repository;

import de.hochschule.bochum.common.model.SiemensData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// Repository für Siemens-Daten – bietet spezielle Abfragen, die ich fürs Frontend brauche
@Repository
public interface SiemensDataRepository extends MongoRepository<SiemensData, String> {
    // Neuester Eintrag eines bestimmten Typs (IST, SOLL, DIFFERENZ)
    Optional<SiemensData> findTopByTypeOrderByTimestampDesc(String type);

    // Alle Einträge eines Typs, sortiert nach Zeit
    List<SiemensData> findByTypeOrderByTimestampDesc(String type);
}
