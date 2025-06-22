package de.hochschule.bochum.mqttconsumer.service;

import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.mqttconsumer.repository.SiemensDataRepository;
import de.hochschule.bochum.mqttconsumer.repository.WagoDataRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;

@Slf4j
@MessageEndpoint
@RequiredArgsConstructor
public class MqttMessageHandler {

    private final WagoDataRepository wagoRepository;
    private final SiemensDataRepository siemensRepository;
    private final MeterRegistry meterRegistry;

    @Value("${mqtt.topics.wago.status}")
    private String wagoStatusTopic;

    @Value("${mqtt.topics.siemens.ist}")
    private String siemensIstTopic;

    @Value("${mqtt.topics.siemens.soll}")
    private String siemensSollTopic;

    @Value("${mqtt.topics.siemens.differenz}")
    private String siemensDifferenzTopic;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message, @Header("mqtt_receivedTopic") String topic) {
        String payload = message.getPayload().toString();
        log.info("====================================");
        log.info("MQTT MESSAGE RECEIVED!");
        log.info("Topic: {}", topic);
        log.info("Payload: {}", payload);
        log.info("====================================");

        meterRegistry.counter("mqtt.messages.received", "topic", topic).increment();

        try {
            if (topic.equals(wagoStatusTopic)) {
                handleWagoStatus(payload);
            } else if (topic.equals(siemensIstTopic)) {
                handleSiemensTemperature(payload, "IST");
            } else if (topic.equals(siemensSollTopic)) {
                handleSiemensTemperature(payload, "SOLL");
            } else if (topic.equals(siemensDifferenzTopic)) {
                handleSiemensTemperature(payload, "DIFFERENZ");
            } else if (topic.equals("Random/Integer")) {
                // Use Random/Integer as test data
                log.info("Using Random/Integer as test data");
                handleWagoStatus(payload);
                // Also create dummy Siemens data
                Double randomTemp = Double.parseDouble(payload) / 10.0;
                handleSiemensTemperature(String.valueOf(randomTemp + 20), "IST");
                handleSiemensTemperature(String.valueOf(25.0), "SOLL");
                handleSiemensTemperature(String.valueOf(randomTemp - 5), "DIFFERENZ");
            } else {
                log.warn("X Unknown topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("X Error processing message from topic {}: {}", topic, e.getMessage());
        }
    }

    private void handleWagoStatus(String payload) {
        try {
            // Remove brackets if present
            String cleanPayload = payload.trim().replaceAll("[\\[\\]]", "");
            Integer status = Integer.parseInt(cleanPayload);
            WagoData wagoData = new WagoData(status);
            WagoData saved = wagoRepository.save(wagoData);
            log.info("WAGO DATA SAVED!");
            log.info("   Status: {}", status);
            log.info("   Binary: {}", Integer.toBinaryString(status));
            log.info("   ID: {}", saved.getId());
            log.info("   Timestamp: {}", saved.getTimestamp());
        } catch (NumberFormatException e) {
            log.error("X Invalid Wago status format: {}", payload);
        }
    }

    private void handleSiemensTemperature(String payload, String type) {
        try {
            Double temperature = Double.parseDouble(payload.trim());
            SiemensData siemensData = new SiemensData(temperature, type);
            SiemensData saved = siemensRepository.save(siemensData);
            log.info("SIEMENS {} DATA SAVED!", type);
            log.info("   Temperature: {}", temperature);
            log.info("   ID: {}", saved.getId());
            log.info("   Timestamp: {}", saved.getTimestamp());
        } catch (NumberFormatException e) {
            log.error("X Invalid Siemens temperature format: {}", payload);
        }
    }
}