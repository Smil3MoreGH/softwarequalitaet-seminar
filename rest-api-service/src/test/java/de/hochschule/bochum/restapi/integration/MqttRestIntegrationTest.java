package de.hochschule.bochum.restapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.hochschule.bochum.common.dto.ControlCommand;
import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.common.model.WagoData;
import de.hochschule.bochum.restapi.repository.SiemensDataRepository;
import de.hochschule.bochum.restapi.repository.WagoDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.data.mongodb.auto-index-creation=true",
        "mqtt.topics.wago.status=test/wago/status",
        "mqtt.topics.siemens.ist=test/siemens/ist",
        "mqtt.topics.siemens.soll=test/siemens/soll",
        "mqtt.topics.siemens.differenz=test/siemens/differenz"
})
public class MqttRestIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private WagoDataRepository wagoDataRepository;

    @Autowired
    private SiemensDataRepository siemensDataRepository;

    @Autowired
    private MessageChannel mqttInputChannel;

    @MockBean(name = "mqttApiOutbound")
    private MqttPahoMessageHandler mqttOutboundHandler;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // Clean up database before each test
        wagoDataRepository.deleteAll();
        siemensDataRepository.deleteAll();

        // Reset mocks
        Mockito.reset(mqttOutboundHandler);
    }

    /**
     * Test 1: Testet ob eine empfangene MQTT-Nachricht in der Datenbank gespeichert wird
     * und über den REST Service wieder abrufbar ist
     */
    @Test
    void testMqttMessageToDatabase_ThenRetrieveViaRest() throws Exception {
        // Arrange
        String wagoPayload = "123";
        String siemensPayload = "25.5";

        // Act 1: Simuliere MQTT Nachrichten empfangen
        simulateMqttMessage("test/wago/status", wagoPayload);
        simulateMqttMessage("test/siemens/ist", siemensPayload);

        // Warten damit die Nachrichten verarbeitet werden
        Thread.sleep(500);

        // Assert 1: Prüfe ob Daten in der Datenbank gespeichert wurden
        Optional<WagoData> savedWagoData = wagoDataRepository.findTopByOrderByTimestampDesc();
        assertThat(savedWagoData).isPresent();
        assertThat(savedWagoData.get().getStatus()).isEqualTo(123);

        Optional<SiemensData> savedSiemensData = siemensDataRepository.findTopByTypeOrderByTimestampDesc("IST");
        assertThat(savedSiemensData).isPresent();
        assertThat(savedSiemensData.get().getIstTemperatur()).isEqualTo(25.5);
        assertThat(savedSiemensData.get().getType()).isEqualTo("IST");

        // Act 2: Hole Daten über REST API ab
        String wagoResponse = mockMvc.perform(get("/api/wago/status/latest"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String siemensResponse = mockMvc.perform(get("/api/siemens/temperatur/ist/latest"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert 2: Prüfe ob die REST API die korrekten Daten zurückgibt
        WagoData retrievedWagoData = objectMapper.readValue(wagoResponse, WagoData.class);
        assertThat(retrievedWagoData.getStatus()).isEqualTo(123);
        assertThat(retrievedWagoData.getId()).isEqualTo(savedWagoData.get().getId());

        SiemensData retrievedSiemensData = objectMapper.readValue(siemensResponse, SiemensData.class);
        assertThat(retrievedSiemensData.getIstTemperatur()).isEqualTo(25.5);
        assertThat(retrievedSiemensData.getType()).isEqualTo("IST");
        assertThat(retrievedSiemensData.getId()).isEqualTo(savedSiemensData.get().getId());
    }

    /**
     * Test 2: Testet ob ein POST-Request (Control-Befehl) an den REST Service
     * das Senden einer MQTT-Nachricht an die Wago-SPS auslöst
     */
    @Test
    void testRestControlCommand_TriggersMqttMessage() throws Exception {
        // Arrange
        ControlCommand controlCommand = new ControlCommand();
        controlCommand.setCommand(1); // Integer command
        String requestBody = objectMapper.writeValueAsString(controlCommand);

        // Act: Sende POST-Request an REST API
        mockMvc.perform(post("/api/wago/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string("Command sent: 1"));

        // Assert: Prüfe ob MQTT-Nachricht gesendet wurde
        // Verifiziere dass der gemockte Handler aufgerufen wurde
        verify(mqttOutboundHandler, times(1)).handleMessage(any(Message.class));
    }

    /**
     * Test 3: Zusätzlicher Test für verschiedene Siemens-Datentypen
     */
    @Test
    void testMultipleSiemensDataTypes_StorageAndRetrieval() throws Exception {
        // Arrange
        String sollPayload = "22.0";
        String differenzPayload = "3.5";

        // Act: Simuliere verschiedene Siemens MQTT Nachrichten sequenziell
        simulateMqttMessage("test/siemens/soll", sollPayload);
        Thread.sleep(200); // Kurze Pause zwischen den Nachrichten
        simulateMqttMessage("test/siemens/differenz", differenzPayload);

        Thread.sleep(500); // Warten auf Verarbeitung

        // Assert: Prüfe Datenbankeinträge
        Optional<SiemensData> sollData = siemensDataRepository.findTopByTypeOrderByTimestampDesc("SOLL");
        Optional<SiemensData> differenzData = siemensDataRepository.findTopByTypeOrderByTimestampDesc("DIFFERENZ");

        assertThat(sollData).isPresent();
        assertThat(sollData.get().getSollTemperatur()).isEqualTo(22.0);
        assertThat(sollData.get().getType()).isEqualTo("SOLL");

        assertThat(differenzData).isPresent();
        assertThat(differenzData.get().getDifferenzTemperatur()).isEqualTo(3.5);
        assertThat(differenzData.get().getType()).isEqualTo("DIFFERENZ");

        // Teste REST API Abruf
        mockMvc.perform(get("/api/siemens/temperatur/soll/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sollTemperatur").value(22.0))
                .andExpect(jsonPath("$.type").value("SOLL"));

        mockMvc.perform(get("/api/siemens/temperatur/differenz/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.differenzTemperatur").value(3.5))
                .andExpect(jsonPath("$.type").value("DIFFERENZ"));
    }

    /**
     * Hilfsmethode um MQTT-Nachrichten zu simulieren
     */
    private void simulateMqttMessage(String topic, String payload) {
        Message<String> message = MessageBuilder.withPayload(payload)
                .setHeader("mqtt_receivedTopic", topic)
                .build();

        mqttInputChannel.send(message);
    }
}