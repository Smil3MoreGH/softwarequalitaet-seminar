package de.hochschule.bochum.restapi.service;

import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.restapi.repository.SiemensDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für SiemensService.
 *
 * Ich teste hier, ob mein Service die Daten korrekt aus dem Repository holt.
 * Es wird geprüft, ob der richtige Wert (bzw. eine leere Liste) zurückkommt
 * und ob das Mapping (Typ und Wert) korrekt funktioniert.
 */
@ExtendWith(MockitoExtension.class)
class SiemensServiceTest {

    @Mock
    private SiemensDataRepository siemensRepository;

    @InjectMocks
    private SiemensService siemensService;

    private SiemensData istData;
    private SiemensData sollData;
    private SiemensData differenzData;

    @BeforeEach
    void setUp() {
        istData = new SiemensData(23.5, "IST");
        istData.setId("ist-123");

        sollData = new SiemensData(25.0, "SOLL");
        sollData.setId("soll-456");

        differenzData = new SiemensData(-1.5, "DIFFERENZ");
        differenzData.setId("diff-789");
    }

    @Test
    void testGetLatestByType_ShouldReturnLatestIstData() {
        // **Was mache ich?**
        // Ich simuliere, dass das Repository für Typ "IST" den passenden Eintrag liefert.

        when(siemensRepository.findTopByTypeOrderByTimestampDesc("IST"))
                .thenReturn(Optional.of(istData));

        Optional<SiemensData> result = siemensService.getLatestByType("IST");

        // **Was erwarte ich?**
        // - Das Ergebnis ist vorhanden, ID und Typ stimmen, und nur der Ist-Wert ist gesetzt
        assertTrue(result.isPresent());
        assertEquals("ist-123", result.get().getId());
        assertEquals("IST", result.get().getType());
        assertEquals(Double.valueOf(23.5), result.get().getIstTemperatur());
        assertNull(result.get().getSollTemperatur());
        assertNull(result.get().getDifferenzTemperatur());
        verify(siemensRepository, times(1)).findTopByTypeOrderByTimestampDesc("IST");
    }

    @Test
    void testGetLatestByType_ShouldReturnLatestSollData() {
        // **Was mache ich?**
        // Simuliere Repository-Return für Typ "SOLL".

        when(siemensRepository.findTopByTypeOrderByTimestampDesc("SOLL"))
                .thenReturn(Optional.of(sollData));

        Optional<SiemensData> result = siemensService.getLatestByType("SOLL");

        // **Was erwarte ich?**
        // - Ergebnis vorhanden, Typ stimmt, nur Soll-Wert gesetzt
        assertTrue(result.isPresent());
        assertEquals("SOLL", result.get().getType());
        assertEquals(Double.valueOf(25.0), result.get().getSollTemperatur());
        assertNull(result.get().getIstTemperatur());
        verify(siemensRepository, times(1)).findTopByTypeOrderByTimestampDesc("SOLL");
    }

    @Test
    void testGetLatestByType_ShouldReturnEmptyWhenNoData() {
        // **Was mache ich?**
        // Simuliere, dass das Repository keine Daten findet.

        when(siemensRepository.findTopByTypeOrderByTimestampDesc("IST"))
                .thenReturn(Optional.empty());

        Optional<SiemensData> result = siemensService.getLatestByType("IST");

        // **Was erwarte ich?**
        // - Ergebnis ist nicht vorhanden (Optional.empty)
        assertFalse(result.isPresent());
        verify(siemensRepository, times(1)).findTopByTypeOrderByTimestampDesc("IST");
    }

    @Test
    void testGetAllByType_ShouldReturnAllDataOfType() {
        // **Was mache ich?**
        // Ich simuliere, dass für einen Typ mehrere Werte geliefert werden.

        List<SiemensData> expectedList = Arrays.asList(istData, istData);
        when(siemensRepository.findByTypeOrderByTimestampDesc("IST"))
                .thenReturn(expectedList);

        List<SiemensData> result = siemensService.getAllByType("IST");

        // **Was erwarte ich?**
        // - Liste ist nicht leer, enthält nur Einträge vom Typ IST
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("IST", result.get(0).getType());
        assertEquals("IST", result.get(1).getType());
        verify(siemensRepository, times(1)).findByTypeOrderByTimestampDesc("IST");
    }
}
