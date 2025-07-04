package de.hochschule.bochum.mqttconsumer.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

// Hier konfiguriere ich die komplette MQTT-Anbindung für meinen Service
@Configuration
public class MqttConfig {

    // Konfiguration aus application.properties holen (Broker, User, PW usw.)
    @Value("${mqtt.broker.url}")
    private String brokerUrl;
    @Value("${mqtt.broker.username}")
    private String username;
    @Value("${mqtt.broker.password}")
    private String password;
    @Value("${mqtt.broker.client-id}")
    private String clientId;
    @Value("${mqtt.topics.wago.status}")
    private String wagoStatusTopic;
    @Value("${mqtt.topics.siemens.ist}")
    private String siemensIstTopic;
    @Value("${mqtt.topics.siemens.soll}")
    private String siemensSollTopic;
    @Value("${mqtt.topics.siemens.differenz}")
    private String siemensDifferenzTopic;
    @Value("${mqtt.topics.test}")
    private String testTopic;

    // MQTT-Client-Factory mit meinen Broker-Settings (inkl. Auth)
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
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

    // Hier landen alle eingehenden MQTT-Nachrichten
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    // Für ausgehende MQTT-Nachrichten (z.B. Steuerbefehle)
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    // Adapter für eingehende MQTT-Nachrichten: Abonniert alle relevanten Topics
    @Bean
    public MqttPahoMessageDrivenChannelAdapter inbound() {
        String[] topics = {
                wagoStatusTopic,
                siemensIstTopic,
                siemensSollTopic,
                siemensDifferenzTopic,
                testTopic
        };

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        clientId + "-inbound",
                        mqttClientFactory(),
                        topics);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    // Handler für ausgehende MQTT-Nachrichten (wird per @ServiceActivator automatisch benutzt)
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(clientId + "-outbound", mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(1);
        return messageHandler;
    }
}
