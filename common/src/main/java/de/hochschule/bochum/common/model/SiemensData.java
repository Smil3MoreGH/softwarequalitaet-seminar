package de.hochschule.bochum.common.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "siemens_data")
public class SiemensData {
    @Id
    private String id;
    private Double istTemperatur;
    private Double sollTemperatur;
    private Double differenzTemperatur;
    private String type; // "IST", "SOLL", "DIFFERENZ"
    private LocalDateTime timestamp;

    public SiemensData(Double value, String type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();

        switch(type) {
            case "IST":
                this.istTemperatur = value;
                break;
            case "SOLL":
                this.sollTemperatur = value;
                break;
            case "DIFFERENZ":
                this.differenzTemperatur = value;
                break;
        }
    }
}