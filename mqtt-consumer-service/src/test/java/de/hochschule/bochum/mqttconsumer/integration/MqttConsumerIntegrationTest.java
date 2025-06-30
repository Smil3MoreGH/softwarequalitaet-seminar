package de.hochschule.bochum.mqttconsumer.integration;

import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.mqttconsumer.repository.MqttSiemensDataRepository;
import de.hochschule.bochum.mqttconsumer.repository.MqttWagoDataRepository;
import de.hochschule.bochum.mqttconsumer.service.MqttMessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class MqttConsumerIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        // Set the topic values that MqttMessageHandler expects
        registry.add("mqtt.topics.wago.status", () -> "mqtt/Wago/status");
        registry.add("mqtt.topics.siemens.ist", () -> "mqtt/controller/Istwert");
        registry.add("mqtt.topics.siemens.soll", () -> "mqtt/controller/Sollwert");
        registry.add("mqtt.topics.siemens.differenz", () -> "mqtt/controller/Differenz");
    }

    @Autowired
    private MqttMessageHandler mqttMessageHandler;

    @Autowired
    private MqttSiemensDataRepository MqttSiemensDataRepository;

    @Autowired
    private MqttWagoDataRepository MqttWagoDataRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        MqttSiemensDataRepository.deleteAll();
        MqttWagoDataRepository.deleteAll();
    }

    @Test
    void testSiemensIstTemperaturMessageIsStoredInDatabase() {
        // Given
        String topic = "mqtt/controller/Istwert";
        String payload = "23.5";

        // Create a mock message
        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader("mqtt_receivedTopic", topic)
                .build();

        // When - Simulate receiving MQTT message
        mqttMessageHandler.handleMessage(message, topic);

        // Then - Verify data is stored in database
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<SiemensData> allData = MqttSiemensDataRepository.findAll();
            assertEquals(1, allData.size());

            SiemensData storedData = allData.get(0);
            assertNotNull(storedData);
            assertEquals("IST", storedData.getType());
            assertEquals(23.5, storedData.getIstTemperatur());
            assertNull(storedData.getSollTemperatur());
            assertNull(storedData.getDifferenzTemperatur());
            assertNotNull(storedData.getTimestamp());
        });
    }

    @Test
    void testSiemensSollTemperaturMessageIsStoredInDatabase() {
        // Given
        String topic = "mqtt/controller/Sollwert";
        String payload = "25.0";

        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader("mqtt_receivedTopic", topic)
                .build();

        // When
        mqttMessageHandler.handleMessage(message, topic);

        // Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<SiemensData> allData = MqttSiemensDataRepository.findAll();
            assertEquals(1, allData.size());

            SiemensData storedData = allData.get(0);
            assertEquals("SOLL", storedData.getType());
            assertEquals(25.0, storedData.getSollTemperatur());
            assertNull(storedData.getIstTemperatur());
            assertNull(storedData.getDifferenzTemperatur());
        });
    }

    @Test
    void testSiemensDifferenzTemperaturMessageIsStoredInDatabase() {
        // Given
        String topic = "mqtt/controller/Differenz";
        String payload = "1.5";

        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader("mqtt_receivedTopic", topic)
                .build();

        // When
        mqttMessageHandler.handleMessage(message, topic);

        // Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<SiemensData> allData = MqttSiemensDataRepository.findAll();
            assertEquals(1, allData.size());

            SiemensData storedData = allData.get(0);
            assertEquals("DIFFERENZ", storedData.getType());
            assertEquals(1.5, storedData.getDifferenzTemperatur());
            assertNull(storedData.getIstTemperatur());
            assertNull(storedData.getSollTemperatur());
        });
    }

    @Test
    void testWagoStatusMessageIsStoredInDatabase() {
        // Given
        String topic = "mqtt/Wago/status";
        String payload = "42";

        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader("mqtt_receivedTopic", topic)
                .build();

        // When
        mqttMessageHandler.handleMessage(message, topic);

        // Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<WagoData> allData = MqttWagoDataRepository.findAll();
            assertEquals(1, allData.size());

            WagoData storedData = allData.get(0);
            assertNotNull(storedData);
            assertEquals(42, storedData.getStatus());
            assertNotNull(storedData.getStatusBinary());
            assertNotNull(storedData.getTimestamp());

            // Verify binary conversion
            byte[] expectedBinary = new byte[] { 0, 42 };
            assertArrayEquals(expectedBinary, storedData.getStatusBinary());
        });
    }

    @Test
    void testMultipleMessagesAreStoredCorrectly() {
        // Given
        String[] topics = {
                "mqtt/controller/Istwert",
                "mqtt/controller/Sollwert",
                "mqtt/controller/Differenz",
                "mqtt/Wago/status"
        };
        String[] payloads = {"20.0", "22.0", "2.0", "15"};

        // When - Send multiple messages
        for (int i = 0; i < topics.length; i++) {
            Message<String> message = MessageBuilder.withPayload(payloads[i])
                    .setHeader("mqtt_receivedTopic", topics[i])
                    .build();
            mqttMessageHandler.handleMessage(message, topics[i]);
        }

        // Then - Verify all data is stored
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<SiemensData> siemensData = MqttSiemensDataRepository.findAll();
            List<WagoData> wagoData = MqttWagoDataRepository.findAll();

            assertEquals(3, siemensData.size());
            assertEquals(1, wagoData.size());

            // Verify each type exists
            assertTrue(siemensData.stream().anyMatch(d -> "IST".equals(d.getType())));
            assertTrue(siemensData.stream().anyMatch(d -> "SOLL".equals(d.getType())));
            assertTrue(siemensData.stream().anyMatch(d -> "DIFFERENZ".equals(d.getType())));
        });
    }

    @Test
    void testInvalidPayloadIsHandledGracefully() {
        // Given
        String topic = "mqtt/controller/Istwert";
        String invalidPayload = "not-a-number";

        Message<String> message = MessageBuilder.withPayload(invalidPayload)
                .setHeader("mqtt_receivedTopic", topic)
                .build();

        // When
        mqttMessageHandler.handleMessage(message, topic);

        // Then - Verify no data is stored
        await().during(2, TimeUnit.SECONDS).untilAsserted(() -> {
            List<SiemensData> allData = MqttSiemensDataRepository.findAll();
            assertEquals(0, allData.size());
        });
    }

    @Test
    void testRandomIntegerTopicCreatesMultipleDataPoints() {
        // Given - Test the Random/Integer topic handling
        String topic = "Random/Integer";
        String payload = "100";

        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader("mqtt_receivedTopic", topic)
                .build();

        // When
        mqttMessageHandler.handleMessage(message, topic);

        // Then - Should create both Wago and multiple Siemens data points
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<SiemensData> siemensData = MqttSiemensDataRepository.findAll();
            List<WagoData> wagoData = MqttWagoDataRepository.findAll();

            // Should create 1 Wago data and 3 Siemens data points
            assertEquals(3, siemensData.size());
            assertEquals(1, wagoData.size());

            // Verify Wago data
            assertEquals(100, wagoData.get(0).getStatus());

            // Verify Siemens data types
            assertTrue(siemensData.stream().anyMatch(d -> "IST".equals(d.getType())));
            assertTrue(siemensData.stream().anyMatch(d -> "SOLL".equals(d.getType())));
            assertTrue(siemensData.stream().anyMatch(d -> "DIFFERENZ".equals(d.getType())));
        });
    }
}