package de.hochschule.bochum.restapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hochschule.bochum.common.dto.ControlCommand;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
 * Integrationstest zu Aufgabe 9b:
 *
 * Hier teste ich die gesamte Prozesskette für einen Steuerbefehl:
 * 1. Es wird ein REST-API POST auf /api/wago/control abgesetzt.
 * 2. Es soll im Hintergrund eine MQTT-Nachricht mit dem Befehl an das Topic "Wago750/Control" gesendet werden.
 * 3. Ich lausche selbst mit einem MQTT-Client auf diesem Topic und prüfe, ob der Befehl tatsächlich eintrifft.
 *
 * Ziel: Sicherstellen, dass ein REST-Aufruf tatsächlich das Senden einer MQTT-Nachricht auslöst.
 * Es werden Testcontainer für Mosquitto (MQTT-Broker) und MongoDB verwendet.
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
        // Konfiguriere dynamisch die MQTT-Broker-URL für den Spring Context
        r.add("mqtt.broker.url", () -> "tcp://localhost:" + mosquitto.getMappedPort(1883));
        r.add("mqtt.broker.clientId", () -> "rest-api-test-client");
        r.add("mqtt.topics.wago.control", () -> "Wago750/Control");

        // Setze MongoDB-Konfiguration auf den Testcontainer
        r.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        r.add("spring.data.mongodb.database", () -> "test-db");
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void clearDatabase() {
        // Vor jedem Test werden alle Collections geleert (außer System-Collections)
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
        // **Was mache ich?**
        // Ich setze einen REST-POST-Befehl ab und prüfe, ob eine MQTT-Nachricht korrekt gesendet wird.

        // Warte kurz, damit alle Container/Services bereit sind
        Thread.sleep(1000);

        // MQTT-Client vorbereitet, um auf das Steuer-Topic zu lauschen
        String brokerUrl = "tcp://localhost:" + mosquitto.getMappedPort(1883);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> nachricht = new AtomicReference<>();

        MqttClient client = new MqttClient(brokerUrl, "test-sub", null);
        client.connect();
        client.subscribe("Wago750/Control", (t, m) -> {
            nachricht.set(new String(m.getPayload()));
            latch.countDown();
        });

        // Erzeuge das ControlCommand, das gesendet werden soll
        ControlCommand cmd = new ControlCommand();
        cmd.setCommand(2);

        // Sende POST-Request an REST-API
        mockMvc.perform(post("/api/wago/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(content().string("Command sent: 2"));

        // Warte bis zu 3 Sekunden, dass wirklich eine Nachricht empfangen wird
        assertTrue(latch.await(3, TimeUnit.SECONDS),
                "MQTT-Nachricht sollte innerhalb von 3 Sekunden empfangen werden");

        // Prüfe, ob der korrekte Wert als Payload angekommen ist
        assertEquals("2", nachricht.get());

        client.disconnect();
        client.close();
    }
}
