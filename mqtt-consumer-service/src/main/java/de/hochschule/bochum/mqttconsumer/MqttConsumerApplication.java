package de.hochschule.bochum.mqttconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MqttConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MqttConsumerApplication.class, args);
    }
}