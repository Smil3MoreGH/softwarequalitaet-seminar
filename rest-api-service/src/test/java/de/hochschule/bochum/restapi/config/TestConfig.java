package de.hochschule.bochum.restapi.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;

/**
 * Test-Konfiguration:
 * - Stellt Mock-Beans für MQTT bereit, damit bei Tests keine echten Nachrichten gesendet werden.
 * - Aktiv wird diese Config nur im Test-Profil ("test").
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    // Hier ersetze ich den echten MQTT MessageHandler durch einen Mock für meine Tests
    @Bean(name = "mqttApiOutbound")
    @Primary
    public MqttPahoMessageHandler mqttApiOutbound() {
        return Mockito.mock(MqttPahoMessageHandler.class);
    }

    // Channel zum Testen – wird gebraucht, falls im Test ein Channel erwartet wird
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }
}
