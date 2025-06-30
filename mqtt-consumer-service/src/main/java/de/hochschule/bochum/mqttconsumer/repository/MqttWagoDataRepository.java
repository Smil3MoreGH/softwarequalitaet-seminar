package de.hochschule.bochum.mqttconsumer.repository;

import de.hochschule.bochum.common.model.WagoData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MqttWagoDataRepository extends MongoRepository<WagoData, String> {
    Optional<WagoData> findTopByOrderByTimestampDesc();
}