/*package de.hochschule.bochum.restapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hochschule.bochum.common.dto.ControlCommand;
import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.restapi.repository.SiemensDataRepository;
import de.hochschule.bochum.restapi.repository.WagoDataRepository;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class RestApiIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017);

    @Container
    static GenericContainer<?> mosquittoContainer = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:latest"))
            .withExposedPorts(1883)
            .withCommand("mosquitto", "-c", "/mosquitto-no-auth.conf");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("mqtt.broker.url", () -> "tcp://localhost:" + mosquittoContainer.getMappedPort(1883));
        registry.add("mqtt.broker.clientId", () -> "test-client");
        registry.add("mqtt.topics.siemens.ist", () -> "mqtt/controller/Istwert");
        registry.add("mqtt.topics.siemens.soll", () -> "mqtt/controller/Sollwert");
        registry.add("mqtt.topics.siemens.differenz", () -> "mqtt/controller/Differenz");
        registry.add("mqtt.topics.wago.control", () -> "mqtt/Wago/control");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SiemensDataRepository siemensDataRepository;

    @Autowired
    private WagoDataRepository wagoDataRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        siemensDataRepository.deleteAll();
        wagoDataRepository.deleteAll();
    }

    @Test
    void testGetLatestIstTemperaturFromDatabase() throws Exception {
        // Given - Prepare test data in database
        SiemensData testData = new SiemensData(24.5, "IST");
        testData.setTimestamp(LocalDateTime.now());
        siemensDataRepository.save(testData);

        // When & Then - Call REST API and verify response
        mockMvc.perform(get("/api/siemens/temperatur/ist/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.istTemperatur").value(24.5))
                .andExpect(jsonPath("$.type").value("IST"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testGetLatestSollTemperaturFromDatabase() throws Exception {
        // Given
        SiemensData testData = new SiemensData(26.0, "SOLL");
        testData.setTimestamp(LocalDateTime.now());
        siemensDataRepository.save(testData);

        // When & Then
        mockMvc.perform(get("/api/siemens/temperatur/soll/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sollTemperatur").value(26.0))
                .andExpect(jsonPath("$.type").value("SOLL"));
    }

    @Test
    void testGetLatestDifferenzTemperaturFromDatabase() throws Exception {
        // Given
        SiemensData testData = new SiemensData(1.5, "DIFFERENZ");
        testData.setTimestamp(LocalDateTime.now());
        siemensDataRepository.save(testData);

        // When & Then
        mockMvc.perform(get("/api/siemens/temperatur/differenz/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.differenzTemperatur").value(1.5))
                .andExpect(jsonPath("$.type").value("DIFFERENZ"));
    }

    @Test
    void testGetAllIstTemperaturFromDatabase() throws Exception {
        // Given - Multiple data points
        for (int i = 0; i < 3; i++) {
            SiemensData data = new SiemensData(20.0 + i, "IST");
            data.setTimestamp(LocalDateTime.now().minusMinutes(i));
            siemensDataRepository.save(data);
        }

        // When & Then
        mockMvc.perform(get("/api/siemens/temperatur/ist/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("IST"))
                .andExpect(jsonPath("$[1].type").value("IST"))
                .andExpect(jsonPath("$[2].type").value("IST"));
    }

    @Test
    void testGetLatestWagoStatusFromDatabase() throws Exception {
        // Given
        WagoData testData = new WagoData(255);
        wagoDataRepository.save(testData);

        // When & Then
        mockMvc.perform(get("/api/wago/status/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(255))
                .andExpect(jsonPath("$.statusBinary").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testPostControlCommandTriggersMqttMessage() throws Exception {
        // Given - Set up MQTT subscriber to verify message is sent
        String brokerUrl = "tcp://localhost:" + mosquittoContainer.getMappedPort(1883);
        String topic = "Wago750/Control"; // Note: Your WagoService uses "Wago750/Control", not "mqtt/Wago/control"
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        MqttClient subscriber = new MqttClient(brokerUrl, "test-subscriber", null);
        subscriber.connect();
        subscriber.subscribe(topic, (t, msg) -> {
            receivedMessage.set(new String(msg.getPayload()));
            messageLatch.countDown();
        });

        // Create control command
        ControlCommand command = new ControlCommand();
        command.setCommand(2); // Valid command between 0-3

        // When - Send POST request
        mockMvc.perform(post("/api/wago/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(content().string("Befehl gesendet: 2"));

        // Then - Verify MQTT message was sent
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "MQTT message was not received");
        assertEquals("2", receivedMessage.get());

        // Cleanup
        subscriber.disconnect();
        subscriber.close();
    }

    @Test
    void testPostControlCommandWithInvalidValueReturnsError() throws Exception {
        // Given - Invalid command value (must be 0-3)
        ControlCommand command = new ControlCommand();
        command.setCommand(5);

        // When & Then - Should return bad request
        mockMvc.perform(post("/api/wago/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testNoDataReturnsNoContent() throws Exception {
        // When & Then - No data in database
        mockMvc.perform(get("/api/siemens/temperatur/ist/latest"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testMultipleDataPointsReturnLatest() throws Exception {
        // Given - Multiple data points with different timestamps
        LocalDateTime now = LocalDateTime.now();

        SiemensData oldData = new SiemensData(20.0, "IST");
        oldData.setTimestamp(now.minusHours(2));
        siemensDataRepository.save(oldData);

        SiemensData latestData = new SiemensData(25.0, "IST");
        latestData.setTimestamp(now);
        siemensDataRepository.save(latestData);

        SiemensData middleData = new SiemensData(22.5, "IST");
        middleData.setTimestamp(now.minusHours(1));
        siemensDataRepository.save(middleData);

        // When & Then - Should return the latest data
        mockMvc.perform(get("/api/siemens/temperatur/ist/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.istTemperatur").value(25.0));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public MqttConnectOptions mqttConnectOptions() {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            return options;
        }
    }
}
 */