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

/**
 * Unit-Tests für den MqttMessageHandler.
 *
 * Diese Tests überprüfen, ob der Handler empfangene MQTT-Nachrichten korrekt verarbeitet:
 * - Je nach Topic wird die passende Methode (z.B. für Wago oder Siemens) aufgerufen.
 * - Die Daten werden im jeweiligen Repository gespeichert.
 * - Für ungültige oder unbekannte Topics/Payloads passiert kein unerwarteter Datenbankzugriff.
 * - Das Micrometer-Counter-Monitoring wird immer hochgezählt.
 *
 * Die Properties (Topics) werden mit Reflection gesetzt, damit sie wie in der echten Anwendung vorhanden sind.
 */

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
        // Setze die @Value Properties wie sie auch bei Spring gesetzt werden würden
        ReflectionTestUtils.setField(mqttMessageHandler, "wagoStatusTopic", "Wago750/Status");
        ReflectionTestUtils.setField(mqttMessageHandler, "siemensIstTopic", "S7_1500/Temperatur/Ist");
        ReflectionTestUtils.setField(mqttMessageHandler, "siemensSollTopic", "S7_1500/Temperatur/Soll");
        ReflectionTestUtils.setField(mqttMessageHandler, "siemensDifferenzTopic", "S7_1500/Temperatur/Differenz");

        // Mock für Counter von Micrometer
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
    }

    @Test
    void testHandleWagoMessage_ShouldSaveWagoData() {
        // **Was mache ich?**
        // Simuliere den Empfang einer gültigen Wago-Nachricht (Topic: Wago750/Status, Payload: "5").
        // Ich prüfe, dass WagoData im Repository gespeichert wird und der Counter hochgezählt wird.

        String payload = "5";
        String topic = "Wago750/Status";
        Message<String> message = MessageBuilder.withPayload(payload).build();

        WagoData savedData = new WagoData(5);
        savedData.setId("saved-wago-123");
        when(wagoRepository.save(any(WagoData.class))).thenReturn(savedData);

        mqttMessageHandler.handleMessage(message, topic);

        // **Was erwarte ich?**
        // - Repository.save wurde mit dem richtigen Wert aufgerufen
        // - Timestamp ist gesetzt
        // - Siemens-Repository wird NICHT angesprochen
        // - Counter wurde erhöht
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
        // **Was mache ich?**
        // Simuliere Empfang eines Siemens-Ist-Werts (Topic: S7_1500/Temperatur/Ist, Payload: "23.5").
        // Prüfe, dass SiemensData gespeichert wird, Counter erhöht wird und WagoRepository unberührt bleibt.

        String payload = "23.5";
        String topic = "S7_1500/Temperatur/Ist";
        Message<String> message = MessageBuilder.withPayload(payload).build();

        SiemensData savedData = new SiemensData(23.5, "IST");
        savedData.setId("saved-siemens-456");
        when(siemensRepository.save(any(SiemensData.class))).thenReturn(savedData);

        mqttMessageHandler.handleMessage(message, topic);

        // **Was erwarte ich?**
        // - Repository.save wurde auf SiemensData aufgerufen
        // - Typ und Wert stimmen, Soll- und Differenz sind null
        // - Wago-Repository bleibt unberührt
        // - Counter wird erhöht
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
        // **Was mache ich?**
        // Simuliere das Test-Topic "Random/Integer" mit Payload "50".
        // Es sollen jeweils ein WagoData (mit Wert 50) und 3x SiemensData gespeichert werden (für IST, SOLL, DIFFERENZ).

        String payload = "50";
        String topic = "Random/Integer";
        Message<String> message = MessageBuilder.withPayload(payload).build();

        when(wagoRepository.save(any(WagoData.class))).thenReturn(new WagoData(50));
        when(siemensRepository.save(any(SiemensData.class))).thenReturn(new SiemensData(25.0, "IST"));

        mqttMessageHandler.handleMessage(message, topic);

        // **Was erwarte ich?**
        // - WagoRepository speichert genau einmal
        // - SiemensRepository speichert 3 mal (einmal pro Wert)
        // - Counter wird erhöht
        verify(wagoRepository, times(1)).save(any(WagoData.class));
        verify(siemensRepository, times(3)).save(any(SiemensData.class)); // IST, SOLL, DIFFERENZ
        verify(counter, times(1)).increment();
    }

    @Test
    void testHandleUnknownTopic_ShouldNotSaveAnyData() {
        // **Was mache ich?**
        // Simuliere einen unbekannten Topic. Es sollen keine Daten gespeichert werden.

        String payload = "test";
        String topic = "Unknown/Topic";
        Message<String> message = MessageBuilder.withPayload(payload).build();

        mqttMessageHandler.handleMessage(message, topic);

        // **Was erwarte ich?**
        // - Kein Aufruf von save auf Wago- oder Siemens-Repository
        // - Counter wird trotzdem erhöht (Monitoring zählt jede eingehende Nachricht)
        verify(wagoRepository, never()).save(any(WagoData.class));
        verify(siemensRepository, never()).save(any(SiemensData.class));
        verify(counter, times(1)).increment();
    }

    @Test
    void testHandleInvalidWagoPayload_ShouldNotSaveData() {
        // **Was mache ich?**
        // Simuliere ein Wago-Topic mit ungültigem Payload ("invalid_number").
        // Die Speicherung muss fehlschlagen, es dürfen keine Daten persistiert werden.

        String payload = "invalid_number";
        String topic = "Wago750/Status";
        Message<String> message = MessageBuilder.withPayload(payload).build();

        mqttMessageHandler.handleMessage(message, topic);

        // **Was erwarte ich?**
        // - Es werden KEINE Daten gespeichert (Exception wird intern gefangen)
        // - Counter wird trotzdem erhöht
        verify(wagoRepository, never()).save(any(WagoData.class));
        verify(siemensRepository, never()).save(any(SiemensData.class));
        verify(counter, times(1)).increment();
    }
}
