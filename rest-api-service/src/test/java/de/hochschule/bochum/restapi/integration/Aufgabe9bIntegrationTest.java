package de.hochschule.bochum.restapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hochschule.bochum.common.dto.ControlCommand;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aufgabe 9b: Integrationstest
 * Testet, ob ein POST-Request (Steuerbefehl) an den RESTful Web Service
 * das Senden einer Nachricht an die Wago-SPS auslöst.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class Aufgabe9bIntegrationTest {

    @Container
    static GenericContainer<?> mosquitto = new GenericContainer<>("eclipse-mosquitto:latest")
            .withExposedPorts(1883)
            .withCommand("mosquitto", "-c", "/mosquitto-no-auth.conf");

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        // MQTT Configuration
        r.add("mqtt.broker.url", () -> "tcp://localhost:" + mosquitto.getMappedPort(1883));
        r.add("mqtt.broker.clientId", () -> "rest-api-test-client");
        r.add("mqtt.topics.wago.control", () -> "Wago750/Control");

        // MongoDB Configuration - use the real MongoDB container
        r.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        r.add("spring.data.mongodb.database", () -> "test-db");
    }
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void clearDatabase() {
        // Alle Collections löschen (bis auf system-internal)
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            if (!collectionName.startsWith("system.")) {
                mongoTemplate.dropCollection(collectionName);
            }
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void integrationTest_PostTriggertMqttNachricht() throws Exception {
        // Warte kurz, damit alle Services hochgefahren sind
        Thread.sleep(1000);

        // Set up MQTT-Subscriber
        String brokerUrl = "tcp://localhost:" + mosquitto.getMappedPort(1883);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> nachricht = new AtomicReference<>();

        MqttClient client = new MqttClient(brokerUrl, "test-sub", null);
        client.connect();
        client.subscribe("Wago750/Control", (t, m) -> {
            nachricht.set(new String(m.getPayload()));
            latch.countDown();
        });

        // POST an REST schicken
        ControlCommand cmd = new ControlCommand();
        cmd.setCommand(2);

        mockMvc.perform(post("/api/wago/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(content().string("Command sent: 2"));

        // Prüfen, ob MQTT-Nachricht empfangen
        assertTrue(latch.await(3, TimeUnit.SECONDS),
                "MQTT-Nachricht sollte innerhalb von 3 Sekunden empfangen werden");
        assertEquals("2", nachricht.get());

        client.disconnect();
        client.close();
    }
}