package de.hochschule.bochum.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "siemens_data")
public class SiemensData {
    // Hier verwende ich die ID, die MongoDB automatisch vergibt.
    @Id
    private String id;
    // Ist-Temperaturwert der SPS (kommt über MQTT rein).
    private Double istTemperatur;
    // Soll-Temperaturwert der SPS.
    private Double sollTemperatur;
    // Berechnete Differenz zwischen Soll und Ist.
    private Double differenzTemperatur;
    // Typ gibt an, um welchen Wert es sich handelt (IST, SOLL, DIFFERENZ).
    private String type;
    // Zeitstempel, wann der Wert empfangen wurde.
    private LocalDateTime timestamp;

    // Konstruktor zum einfachen Erzeugen des Objekts mit Wert und Typ.
    public SiemensData(Double value, String type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();

        // Je nach Typ wird der richtige Wert gesetzt.
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
