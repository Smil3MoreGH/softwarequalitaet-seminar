package de.hochschule.bochum.restapi.service;

import static org.junit.jupiter.api.Assertions.*;

import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.restapi.repository.WagoDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Unit-Tests für WagoService.
 *
 * Hier prüfe ich, ob die Service-Logik korrekt arbeitet:
 * - Holt der Service die Daten richtig aus dem Repository?
 * - Sendet er bei gültigen Steuerbefehlen eine MQTT-Nachricht?
 * - Wirft er Fehler bei ungültigen Befehlen?
 */
@ExtendWith(MockitoExtension.class)
class WagoServiceTest {

    @Mock
    private WagoDataRepository wagoRepository;

    @Mock
    private MessageChannel mqttOutboundChannel;

    @InjectMocks
    private WagoService wagoService;

    private WagoData testWagoData;

    @BeforeEach
    void setUp() {
        testWagoData = new WagoData(5); // Status 5 = Binär 101
        testWagoData.setId("test-id-123");
    }

    @Test
    void testGetLatestStatus_ShouldReturnLatestData() {
        // **Was mache ich?**
        // Simuliere, dass das Repository einen Datensatz liefert.
        // Der Service gibt das gleiche Objekt zurück.

        when(wagoRepository.findTopByOrderByTimestampDesc()).thenReturn(Optional.of(testWagoData));

        Optional<WagoData> result = wagoService.getLatestStatus();

        // **Was erwarte ich?**
        // - Ergebnis ist vorhanden, ID und Status stimmen
        assertTrue(result.isPresent());
        assertEquals("test-id-123", result.get().getId());
        assertEquals(Integer.valueOf(5), result.get().getStatus());
        assertNotNull(result.get().getStatusBinary());
        verify(wagoRepository, times(1)).findTopByOrderByTimestampDesc();
    }

    @Test
    void testGetLatestStatus_ShouldReturnEmptyWhenNoData() {
        // **Was mache ich?**
        // Simuliere, dass das Repository keine Daten liefert.

        when(wagoRepository.findTopByOrderByTimestampDesc()).thenReturn(Optional.empty());

        Optional<WagoData> result = wagoService.getLatestStatus();

        // **Was erwarte ich?**
        // - Ergebnis ist leer
        assertFalse(result.isPresent());
        verify(wagoRepository, times(1)).findTopByOrderByTimestampDesc();
    }

    @Test
    void testSendControlCommand_ShouldSendValidCommand() {
        // **Was mache ich?**
        // Sende einen gültigen Befehl (z.B. 2).
        // Erwarte, dass der Service eine MQTT-Nachricht sendet.

        Integer validCommand = 2;

        wagoService.sendControlCommand(validCommand);

        // **Was erwarte ich?**
        // - Channel.send wurde einmal aufgerufen (mit einer Nachricht)
        verify(mqttOutboundChannel, times(1)).send(any(Message.class));
    }

    @Test
    void testSendControlCommand_ShouldThrowExceptionForInvalidCommand() {
        // **Was mache ich?**
        // Sende einen ungültigen Befehl (z.B. 5). Service soll Fehler werfen.
        Integer invalidCommand = 5; // Nur 0-3 sind erlaubt

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> wagoService.sendControlCommand(invalidCommand)
        );

        // **Was erwarte ich?**
        // - Exception mit richtiger Message
        // - Es wird keine Nachricht gesendet
        assertEquals("Command must be between 0 and 3", exception.getMessage());
        verify(mqttOutboundChannel, never()).send(any(Message.class));
    }

    @Test
    void testSendControlCommand_ShouldThrowExceptionForNegativeCommand() {
        // **Was mache ich?**
        // Sende einen negativen Befehl. Erwartung: Exception, keine Nachricht.

        Integer negativeCommand = -1;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> wagoService.sendControlCommand(negativeCommand)
        );

        // **Was erwarte ich?**
        // - Exception wird geworfen, keine Nachricht wird gesendet
        assertEquals("Command must be between 0 and 3", exception.getMessage());
        verify(mqttOutboundChannel, never()).send(any(Message.class));
    }
}
