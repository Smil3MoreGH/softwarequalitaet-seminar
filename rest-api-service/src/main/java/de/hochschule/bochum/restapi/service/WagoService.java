package de.hochschule.bochum.restapi.service;

import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.restapi.repository.WagoDataRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class WagoService {

    private final WagoDataRepository wagoRepository;
    private final MessageChannel mqttOutboundChannel;

    public WagoService(
            WagoDataRepository wagoRepository,
            @Qualifier("mqttOutboundChannel") MessageChannel mqttOutboundChannel
    ) {
        this.wagoRepository = wagoRepository;
        this.mqttOutboundChannel = mqttOutboundChannel;
    }

    public Optional<WagoData> getLatestStatus() {
        return wagoRepository.findTopByOrderByTimestampDesc();
    }

    public void sendControlCommand(Integer command) {
        if (command < 0 || command > 3) {
            throw new IllegalArgumentException("Command must be between 0 and 3");
        }

        mqttOutboundChannel.send(
                MessageBuilder.withPayload(command.toString())
                        .setHeader(MqttHeaders.TOPIC, "Wago750/Control")
                        .build()
        );
    }
}
