package de.hochschule.bochum.restapi.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean(name = "mqttApiOutbound")
    @Primary
    public MqttPahoMessageHandler mqttApiOutbound() {
        return Mockito.mock(MqttPahoMessageHandler.class);
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }
}