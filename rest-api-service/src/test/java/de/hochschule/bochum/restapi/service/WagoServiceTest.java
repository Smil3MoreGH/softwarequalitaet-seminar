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
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        // Timestamp wird automatisch im Konstruktor gesetzt
    }

    @Test
    void testGetLatestStatus_ShouldReturnLatestData() {
        // Arrange
        when(wagoRepository.findTopByOrderByTimestampDesc()).thenReturn(Optional.of(testWagoData));

        // Act
        Optional<WagoData> result = wagoService.getLatestStatus();

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test-id-123", result.get().getId());
        assertEquals(Integer.valueOf(5), result.get().getStatus());
        assertNotNull(result.get().getStatusBinary());
        verify(wagoRepository, times(1)).findTopByOrderByTimestampDesc();
    }

    @Test
    void testGetLatestStatus_ShouldReturnEmptyWhenNoData() {
        // Arrange
        when(wagoRepository.findTopByOrderByTimestampDesc()).thenReturn(Optional.empty());

        // Act
        Optional<WagoData> result = wagoService.getLatestStatus();

        // Assert
        assertFalse(result.isPresent());
        verify(wagoRepository, times(1)).findTopByOrderByTimestampDesc();
    }

    @Test
    void testSendControlCommand_ShouldSendValidCommand() {
        // Arrange
        Integer validCommand = 2;

        // Act
        wagoService.sendControlCommand(validCommand);

        // Assert
        verify(mqttOutboundChannel, times(1)).send(any(Message.class));
    }

    @Test
    void testSendControlCommand_ShouldThrowExceptionForInvalidCommand() {
        // Arrange
        Integer invalidCommand = 5; // Nur 0-3 sind erlaubt

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> wagoService.sendControlCommand(invalidCommand)
        );

        assertEquals("Command must be between 0 and 3", exception.getMessage());
        verify(mqttOutboundChannel, never()).send(any(Message.class));
    }

    @Test
    void testSendControlCommand_ShouldThrowExceptionForNegativeCommand() {
        // Arrange
        Integer negativeCommand = -1;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> wagoService.sendControlCommand(negativeCommand)
        );

        assertEquals("Command must be between 0 and 3", exception.getMessage());
        verify(mqttOutboundChannel, never()).send(any(Message.class));
    }
}
