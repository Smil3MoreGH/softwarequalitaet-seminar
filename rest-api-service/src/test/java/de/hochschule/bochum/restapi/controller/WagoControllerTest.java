package de.hochschule.bochum.restapi.controller;

import static org.junit.jupiter.api.Assertions.*;

import de.hochschule.bochum.common.dto.ControlCommand;
import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.restapi.service.WagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Unit-Tests für WagoController.
 *
 * In diesen Tests prüfe ich das Verhalten meines Controllers unabhängig von Spring Boot oder
 * einer echten Datenbank. Der Service wird jeweils gemockt.
 *
 * Jeder Test prüft einen bestimmten Endpunkt und das erwartete Verhalten bei Erfolgs- und Fehlerfällen.
 */

@ExtendWith(MockitoExtension.class)
class WagoControllerTest {

    @Mock
    private WagoService wagoService; // Wird für alle Tests als Mock verwendet

    @InjectMocks
    private WagoController wagoController; // Hiermit teste ich die Controller-Logik

    private WagoData testWagoData;
    private ControlCommand controlCommand;

    @BeforeEach
    void setUp() {
        // Initialisiere Beispiel-Daten, die im Test verwendet werden
        testWagoData = new WagoData(7); // Status 7 = Binär 111
        testWagoData.setId("controller-test-123");

        controlCommand = new ControlCommand();
        controlCommand.setCommand(1);
    }

    @Test
    void testGetLatestStatus_ShouldReturnOkWithData() {
        // **Was mache ich?**
        // Ich simuliere, dass der Service ein WagoData-Objekt liefert und prüfe,
        // dass der Controller daraufhin einen OK-Status und die richtigen Daten zurückgibt.

        when(wagoService.getLatestStatus()).thenReturn(Optional.of(testWagoData));

        ResponseEntity<WagoData> response = wagoController.getLatestStatus();

        // **Was erwarte ich?**
        // - Status 200 OK
        // - Im Body steckt das erwartete WagoData-Objekt mit der richtigen ID und Status
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("controller-test-123", response.getBody().getId());
        assertEquals(Integer.valueOf(7), response.getBody().getStatus());
        verify(wagoService, times(1)).getLatestStatus();
    }

    @Test
    void testGetLatestStatus_ShouldReturnNoContentWhenNoData() {
        // **Was mache ich?**
        // Ich simuliere, dass der Service keine Daten liefert.
        // Der Controller soll daraufhin den Status 204 NO CONTENT liefern.

        when(wagoService.getLatestStatus()).thenReturn(Optional.empty());

        ResponseEntity<WagoData> response = wagoController.getLatestStatus();

        // **Was erwarte ich?**
        // - Status 204 NO CONTENT
        // - Body ist null
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(wagoService, times(1)).getLatestStatus();
    }

    @Test
    void testGetLatestStatusWithBinary_ShouldReturnOkWithData() {
        // **Was mache ich?**
        // Wie beim ersten Test, aber für den /status/latest/binary-Endpunkt.
        // Simuliere WagoData, prüfe, dass das Binary-Feld gesetzt ist.

        when(wagoService.getLatestStatus()).thenReturn(Optional.of(testWagoData));

        ResponseEntity<WagoData> response = wagoController.getLatestStatusWithBinary();

        // **Was erwarte ich?**
        // - Status 200 OK
        // - Daten vorhanden, Status stimmt, und Binary ist gesetzt
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Integer.valueOf(7), response.getBody().getStatus());
        assertNotNull(response.getBody().getStatusBinary());
        verify(wagoService, times(1)).getLatestStatus();
    }

    @Test
    void testSendControlCommand_ShouldReturnOkWithSuccessMessage() {
        // **Was mache ich?**
        // Sende einen gültigen Steuerbefehl.
        // Der Service wird so gemockt, dass nichts passiert (doNothing).

        doNothing().when(wagoService).sendControlCommand(1);

        ResponseEntity<String> response = wagoController.sendControlCommand(controlCommand);

        // **Was erwarte ich?**
        // - Status 200 OK
        // - Rückgabewert enthält die Bestätigung für den Command
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Command sent: 1"));
        verify(wagoService, times(1)).sendControlCommand(1);
    }

    @Test
    void testSendControlCommand_ShouldHandleServiceException() {
        // **Was mache ich?**
        // Sende einen ungültigen Command und simuliere, dass der Service eine Exception wirft.

        ControlCommand invalidCommand = new ControlCommand();
        invalidCommand.setCommand(5); // Invalid command

        doThrow(new IllegalArgumentException("Command must be between 0 and 3"))
                .when(wagoService).sendControlCommand(5);

        // **Was erwarte ich?**
        // - Der Controller wirft die Exception weiter (in einer echten Anwendung könnte hier ein @ExceptionHandler greifen)
        assertThrows(IllegalArgumentException.class, () -> {
            wagoController.sendControlCommand(invalidCommand);
        });

        verify(wagoService, times(1)).sendControlCommand(5);
    }
}
