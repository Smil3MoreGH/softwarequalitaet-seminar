package de.hochschule.bochum.mqttconsumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MqttConnectionListener {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @EventListener
    public void handleMqttSubscribed(MqttSubscribedEvent event) {
        log.info("🔗 ====================================");
        log.info("🔗 MQTT CONNECTION SUCCESSFUL!");
        log.info("🔗 Broker: {}", brokerUrl);
        log.info("🔗 Message: {}", event.getMessage());
        log.info("🔗 ====================================");
    }

    @EventListener
    public void handleMqttConnectionFailed(MqttConnectionFailedEvent event) {
        log.error("❌ ====================================");
        log.error("❌ MQTT CONNECTION FAILED!");
        log.error("❌ Broker: {}", brokerUrl);
        log.error("❌ Cause: {}", event.getCause().getMessage());
        log.error("❌ ====================================");
    }
}