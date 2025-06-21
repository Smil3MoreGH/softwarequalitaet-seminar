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
    @Id
    private String id;
    private Integer status;
    private byte[] statusBinary;
    private LocalDateTime timestamp;

    public WagoData(Integer status) {
        this.status = status;
        this.statusBinary = intToByteArray(status);
        this.timestamp = LocalDateTime.now();
    }

    private byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >> 8),
                (byte)value
        };
    }
}
