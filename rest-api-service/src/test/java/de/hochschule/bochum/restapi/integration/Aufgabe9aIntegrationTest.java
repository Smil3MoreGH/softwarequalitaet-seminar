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
 * Integrationstest zu Aufgabe 9a:
 *
 * In diesem Test prüfe ich die End-to-End-Funktionalität meines Systems:
 * 1. Ich speichere einen (simulierten) über MQTT empfangenen Datensatz direkt in die MongoDB.
 * 2. Ich stelle sicher, dass die Daten korrekt in der Datenbank gelandet sind.
 * 3. Ich teste, ob die REST-API die Werte wieder korrekt ausliefert.
 *
 * Ziel: Sicherstellen, dass Daten, die von der SPS über MQTT empfangen wurden, auch wirklich
 * über die REST-API abrufbar sind. Es wird mit echten Containern (Testcontainers) getestet.
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
        // Vor jedem Test alle Siemens-Daten löschen (Clean Start)
        siemensDataRepository.deleteAll();
    }

    @Test
    void integrationTest_EmpfangSpeichernRestAbruf() throws Exception {
        // ========== SCHRITT 1: Simuliere MQTT-Nachrichtenempfang ==========
        // Da in diesem Test kein echter MQTT-Consumer läuft, simuliere ich den Empfang einer Nachricht
        // indem ich direkt einen Datensatz wie vom Handler in die Datenbank schreibe.

        Double empfangeneTemperatur = 23.5;

        SiemensData empfangeneDaten = new SiemensData(empfangeneTemperatur, "IST");
        empfangeneDaten.setTimestamp(LocalDateTime.now());

        // Speichere den Datensatz (so als wäre er per MQTT gekommen)
        SiemensData gespeichert = siemensDataRepository.save(empfangeneDaten);

        // ========== SCHRITT 2: Verifiziere Speicherung in Datenbank ==========
        // Warte kurz, bis die Datenbank garantiert geschrieben hat (asynchron möglich)
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            var alleDaten = siemensDataRepository.findAll();
            assertEquals(1, alleDaten.size(), "Genau ein Datensatz sollte gespeichert sein");

            var daten = alleDaten.get(0);
            assertEquals(23.5, daten.getIstTemperatur());
            assertEquals("IST", daten.getType());
        });

        // ========== SCHRITT 3: Teste REST-API Abruf ==========
        // Jetzt rufe ich den REST-Endpunkt für den aktuellsten Ist-Wert auf
        mockMvc.perform(get("/api/siemens/temperatur/ist/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.istTemperatur").value(23.5))
                .andExpect(jsonPath("$.type").value("IST"))
                .andExpect(jsonPath("$.timestamp").exists());
        // Erwartung: Die REST-API gibt die gespeicherten Werte korrekt zurück.
    }
}
