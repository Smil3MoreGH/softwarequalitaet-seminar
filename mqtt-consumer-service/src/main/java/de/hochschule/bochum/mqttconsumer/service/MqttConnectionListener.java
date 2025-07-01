package de.hochschule.bochum.mqttconsumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.stereotype.Component;

// Mit diesem Listener reagiere ich auf Verbindungsereignisse zum MQTT-Broker
@Slf4j
@Component
public class MqttConnectionListener {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    // Wird aufgerufen, wenn die Verbindung erfolgreich aufgebaut und ein Topic abonniert wurde
    @EventListener
    public void handleMqttSubscribed(MqttSubscribedEvent event) {
        log.info("====================================");
        log.info("MQTT CONNECTION SUCCESSFUL!");
        log.info("Broker: {}", brokerUrl);
        log.info("Message: {}", event.getMessage());
        log.info("====================================");
    }

    // Wird aufgerufen, wenn die Verbindung zum Broker fehlschlägt
    @EventListener
    public void handleMqttConnectionFailed(MqttConnectionFailedEvent event) {
        log.error("X ====================================");
        log.error("X MQTT CONNECTION FAILED!");
        log.error("X Broker: {}", brokerUrl);
        log.error("X Cause: {}", event.getCause().getMessage());
        log.error("X ====================================");
    }
}
