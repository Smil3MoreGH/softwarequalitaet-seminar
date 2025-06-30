package de.hochschule.bochum.mqttconsumer.service;

import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.mqttconsumer.repository.MqttSiemensDataRepository;
import de.hochschule.bochum.mqttconsumer.repository.MqttWagoDataRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttMessageHandlerTest {

    @Mock
    private MqttWagoDataRepository wagoRepository;

    @Mock
    private MqttSiemensDataRepository siemensRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private MqttMessageHandler mqttMessageHandler;

    @BeforeEach
    void setUp() {
        // Set up the @Value properties using ReflectionTestUtils
        ReflectionTestUtils.setField(mqttMessageHandler, "wagoStatusTopic", "Wago750/Status");
        ReflectionTestUtils.setField(mqttMessageHandler, "siemensIstTopic", "S7_1500/Temperatur/Ist");
        ReflectionTestUtils.setField(mqttMessageHandler, "siemensSollTopic", "S7_1500/Temperatur/Soll");
        ReflectionTestUtils.setField(mqttMessageHandler, "siemensDifferenzTopic", "S7_1500/Temperatur/Differenz");

        // Mock the counter
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    }

    @Test
    void testHandleWagoMessage_ShouldSaveWagoData() {
        // Arrange
        String payload = "5";
        String topic = "Wago750/Status";
        Message<String> message = MessageBuilder.withPayload(payload).build();

        WagoData savedData = new WagoData(5);
        savedData.setId("saved-wago-123");
        when(wagoRepository.save(any(WagoData.class))).thenReturn(savedData);

        // Act
        mqttMessageHandler.handleMessage(message, topic);

        // Assert
        ArgumentCaptor<WagoData> wagoCaptor = ArgumentCaptor.forClass(WagoData.class);
        verify(wagoRepository, times(1)).save(wagoCaptor.capture());

        WagoData capturedWago = wagoCaptor.getValue();
        assertEquals(Integer.valueOf(5), capturedWago.getStatus());
        assertNotNull(capturedWago.getTimestamp());

        verify(siemensRepository, never()).save(any(SiemensData.class));
        verify(counter, times(1)).increment();
    }

    @Test
    void testHandleSiemensIstMessage_ShouldSaveSiemensData() {
        // Arrange
        String payload = "23.5";
        String topic = "S7_1500/Temperatur/Ist";
        Message<String> message = MessageBuilder.withPayload(payload).build();

        SiemensData savedData = new SiemensData(23.5, "IST");
        savedData.setId("saved-siemens-456");
        when(siemensRepository.save(any(SiemensData.class))).thenReturn(savedData);

        // Act
        mqttMessageHandler.handleMessage(message, topic);

        // Assert
        ArgumentCaptor<SiemensData> siemensCaptor = ArgumentCaptor.forClass(SiemensData.class);
        verify(siemensRepository, times(1)).save(siemensCaptor.capture());

        SiemensData capturedSiemens = siemensCaptor.getValue();
        assertEquals("IST", capturedSiemens.getType());
        assertEquals(Double.valueOf(23.5), capturedSiemens.getIstTemperatur());
        assertNull(capturedSiemens.getSollTemperatur());
        assertNull(capturedSiemens.getDifferenzTemperatur());

        verify(wagoRepository, never()).save(any(WagoData.class));
        verify(counter, times(1)).increment();
    }

    @Test
    void testHandleRandomIntegerMessage_ShouldSaveBothWagoAndSiemensData() {
        // Arrange
        String payload = "50";
        String topic = "Random/Integer";
        Message<String> message = MessageBuilder.withPayload(payload).build();

        when(wagoRepository.save(any(WagoData.class))).thenReturn(new WagoData(50));
        when(siemensRepository.save(any(SiemensData.class))).thenReturn(new SiemensData(25.0, "IST"));

        // Act
        mqttMessageHandler.handleMessage(message, topic);

        // Assert
        verify(wagoRepository, times(1)).save(any(WagoData.class));
        verify(siemensRepository, times(3)).save(any(SiemensData.class)); // IST, SOLL, DIFFERENZ
        verify(counter, times(1)).increment();
    }

    @Test
    void testHandleUnknownTopic_ShouldNotSaveAnyData() {
        // Arrange
        String payload = "test";
        String topic = "Unknown/Topic";
        Message<String> message = MessageBuilder.withPayload(payload).build();

        // Act
        mqttMessageHandler.handleMessage(message, topic);

        // Assert
        verify(wagoRepository, never()).save(any(WagoData.class));
        verify(siemensRepository, never()).save(any(SiemensData.class));
        verify(counter, times(1)).increment(); // Counter should still be incremented
    }

    @Test
    void testHandleInvalidWagoPayload_ShouldNotSaveData() {
        // Arrange
        String payload = "invalid_number";
        String topic = "Wago750/Status";
        Message<String> message = MessageBuilder.withPayload(payload).build();

        // Act
        mqttMessageHandler.handleMessage(message, topic);

        // Assert
        verify(wagoRepository, never()).save(any(WagoData.class));
        verify(siemensRepository, never()).save(any(SiemensData.class));
        verify(counter, times(1)).increment();
    }
}