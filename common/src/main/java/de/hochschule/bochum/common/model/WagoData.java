package de.hochschule.bochum.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "wago_data")
public class WagoData {
    // MongoDB generiert die ID automatisch.
    @Id
    private String id;
    // Statuswert als Integer, wie er von der Wago SPS kommt.
    private Integer status;
    // Hier speichere ich den Status als BinaryArray ab (für spätere Visualisierung/Auswertung).
    private byte[] statusBinary;
    // Zeitstempel, wann der Status empfangen wurde.
    private LocalDateTime timestamp;

    // Konstruktor, um direkt den Status zu setzen.
    public WagoData(Integer status) {
        this.status = status;
        // Den Integer-Status direkt in ein 2-Byte Array umwandeln.
        this.statusBinary = intToByteArray(status);
        this.timestamp = LocalDateTime.now();
    }

    // Hilfsmethode zum Umwandeln von Integer in ein 2-Byte Array (wird von mir im Konstruktor verwendet).
    private byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >> 8),  // High-Byte
                (byte)value          // Low-Byte
        };
    }
}
