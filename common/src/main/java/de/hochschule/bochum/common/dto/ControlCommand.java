package de.hochschule.bochum.common.dto;

import lombok.Data;

@Data
public class ControlCommand {
    // Hier speichere ich den Befehl (z.B. 0, 1, 2, 3) für die Wago-Steuerung ab.
    private Integer command;
}
