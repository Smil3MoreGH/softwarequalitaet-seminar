package de.hochschule.bochum.restapi.integration;

import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.restapi.repository.SiemensDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aufgabe 9a: Integrationstest
 * Testet, ob eine empfangene Nachricht in der Datenbank gespeichert
 * und über den RESTful Web Service korrekt abgerufen werden kann.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class Aufgabe9aIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private SiemensDataRepository siemensDataRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        siemensDataRepository.deleteAll();
    }

    @Test
    void integrationTest_EmpfangSpeichernRestAbruf() throws Exception {
        // ========== SCHRITT 1: Simuliere MQTT-Nachrichtenempfang ==========
        // Da wir im REST-Service sind, simulieren wir den Empfang durch direktes Speichern
        // (In der Realität würde der MQTT-Consumer die Nachricht empfangen und speichern)

        Double empfangeneTemperatur = 23.5;
        String topic = "mqtt/controller/Istwert";

        // Simuliere die Verarbeitung einer MQTT-Nachricht
        SiemensData empfangeneDaten = new SiemensData(empfangeneTemperatur, "IST");
        empfangeneDaten.setTimestamp(LocalDateTime.now());

        // Speichere in DB (wie es der echte MQTT-Handler tun würde)
        SiemensData gespeichert = siemensDataRepository.save(empfangeneDaten);

        // ========== SCHRITT 2: Verifiziere Speicherung in Datenbank ==========
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            var alleDaten = siemensDataRepository.findAll();
            assertEquals(1, alleDaten.size(), "Genau ein Datensatz sollte gespeichert sein");

            var daten = alleDaten.get(0);
            assertEquals(23.5, daten.getIstTemperatur());
            assertEquals("IST", daten.getType());
        });

        // ========== SCHRITT 3: Teste REST-API Abruf ==========
        mockMvc.perform(get("/api/siemens/temperatur/ist/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.istTemperatur").value(23.5))
                .andExpect(jsonPath("$.type").value("IST"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}