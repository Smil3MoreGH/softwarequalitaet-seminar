package de.hochschule.bochum.mqttconsumer.service;

import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.mqttconsumer.repository.SiemensDataRepository;
import de.hochschule.bochum.mqttconsumer.repository.WagoDataRepository;
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
        log.debug("Received message from topic {}: {}", topic, payload);

        try {
            if (topic.equals(wagoStatusTopic)) {
                handleWagoStatus(payload);
            } else if (topic.equals(siemensIstTopic)) {
                handleSiemensTemperature(payload, "IST");
            } else if (topic.equals(siemensSollTopic)) {
                handleSiemensTemperature(payload, "SOLL");
            } else if (topic.equals(siemensDifferenzTopic)) {
                handleSiemensTemperature(payload, "DIFFERENZ");
            } else {
                log.warn("Unknown topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error processing message from topic {}: {}", topic, e.getMessage());
        }
    }

    private void handleWagoStatus(String payload) {
        try {
            Integer status = Integer.parseInt(payload.trim());
            WagoData wagoData = new WagoData(status);
            wagoRepository.save(wagoData);
            log.info("Saved Wago status: {}", status);
        } catch (NumberFormatException e) {
            log.error("Invalid Wago status format: {}", payload);
        }
    }

    private void handleSiemensTemperature(String payload, String type) {
        try {
            Double temperature = Double.parseDouble(payload.trim());
            SiemensData siemensData = new SiemensData(temperature, type);
            siemensRepository.save(siemensData);
            log.info("Saved Siemens {} temperature: {}", type, temperature);
        } catch (NumberFormatException e) {
            log.error("Invalid Siemens temperature format: {}", payload);
        }
    }
}