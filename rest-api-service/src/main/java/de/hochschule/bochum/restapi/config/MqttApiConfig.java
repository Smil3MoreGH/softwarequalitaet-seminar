package de.hochschule.bochum.restapi.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

// Hier konfiguriere ich MQTT für meinen REST-API-Service (nur für Outbound/Publish!)
@Configuration
public class MqttApiConfig {

    // Werte aus application.properties einlesen
    @Value("${mqtt.broker.url}")
    private String brokerUrl;
    @Value("${mqtt.broker.username}")
    private String username;
    @Value("${mqtt.broker.password}")
    private String password;
    @Value("${mqtt.broker.client-id}")
    private String clientId;

    // MQTT-Client für Outbound-Kommunikation (Steuerbefehle an Wago)
    @Bean
    public MqttPahoClientFactory mqttApiClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { brokerUrl });
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    // Channel für ausgehende MQTT-Messages
    @Bean
    public MessageChannel mqttApiOutboundChannel() {
        return new DirectChannel();
    }

    // Handler zum Veröffentlichen von Nachrichten (wird im Service benutzt)
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttApiOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(clientId, mqttApiClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(1);
        return messageHandler;
    }
}
