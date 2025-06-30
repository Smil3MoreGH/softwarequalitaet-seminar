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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WagoControllerTest {

    @Mock
    private WagoService wagoService;

    @InjectMocks
    private WagoController wagoController;

    private WagoData testWagoData;
    private ControlCommand controlCommand;

    @BeforeEach
    void setUp() {
        testWagoData = new WagoData(7); // Status 7 = Binär 111
        testWagoData.setId("controller-test-123");

        controlCommand = new ControlCommand();
        controlCommand.setCommand(1);
    }

    @Test
    void testGetLatestStatus_ShouldReturnOkWithData() {
        // Arrange
        when(wagoService.getLatestStatus()).thenReturn(Optional.of(testWagoData));

        // Act
        ResponseEntity<WagoData> response = wagoController.getLatestStatus();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("controller-test-123", response.getBody().getId());
        assertEquals(Integer.valueOf(7), response.getBody().getStatus());
        verify(wagoService, times(1)).getLatestStatus();
    }

    @Test
    void testGetLatestStatus_ShouldReturnNoContentWhenNoData() {
        // Arrange
        when(wagoService.getLatestStatus()).thenReturn(Optional.empty());

        // Act
        ResponseEntity<WagoData> response = wagoController.getLatestStatus();

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(wagoService, times(1)).getLatestStatus();
    }

    @Test
    void testGetLatestStatusWithBinary_ShouldReturnOkWithData() {
        // Arrange
        when(wagoService.getLatestStatus()).thenReturn(Optional.of(testWagoData));

        // Act
        ResponseEntity<WagoData> response = wagoController.getLatestStatusWithBinary();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Integer.valueOf(7), response.getBody().getStatus());
        assertNotNull(response.getBody().getStatusBinary());
        verify(wagoService, times(1)).getLatestStatus();
    }

    @Test
    void testSendControlCommand_ShouldReturnOkWithSuccessMessage() {
        // Arrange
        doNothing().when(wagoService).sendControlCommand(1);

        // Act
        ResponseEntity<String> response = wagoController.sendControlCommand(controlCommand);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Command sent: 1"));
        verify(wagoService, times(1)).sendControlCommand(1);
    }

    @Test
    void testSendControlCommand_ShouldHandleServiceException() {
        // Arrange
        ControlCommand invalidCommand = new ControlCommand();
        invalidCommand.setCommand(5); // Invalid command
        doThrow(new IllegalArgumentException("Command must be between 0 and 3"))
                .when(wagoService).sendControlCommand(5);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            wagoController.sendControlCommand(invalidCommand);
        });

        verify(wagoService, times(1)).sendControlCommand(5);
    }
}