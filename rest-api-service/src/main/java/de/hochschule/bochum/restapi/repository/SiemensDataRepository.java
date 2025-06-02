package de.hochschule.bochum.restapi.repository;

import de.hochschule.bochum.common.model.SiemensData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiemensDataRepository extends MongoRepository<SiemensData, String> {
    Optional<SiemensData> findTopByTypeOrderByTimestampDesc(String type);
    List<SiemensData> findByTypeOrderByTimestampDesc(String type);
}