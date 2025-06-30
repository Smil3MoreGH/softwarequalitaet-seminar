package de.hochschule.bochum.restapi.service;

import de.hochschule.bochum.common.model.SiemensData;
import de.hochschule.bochum.restapi.repository.SiemensDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        // Arrange
        when(siemensRepository.findTopByTypeOrderByTimestampDesc("IST"))
                .thenReturn(Optional.of(istData));

        // Act
        Optional<SiemensData> result = siemensService.getLatestByType("IST");

        // Assert
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
        // Arrange
        when(siemensRepository.findTopByTypeOrderByTimestampDesc("SOLL"))
                .thenReturn(Optional.of(sollData));

        // Act
        Optional<SiemensData> result = siemensService.getLatestByType("SOLL");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("SOLL", result.get().getType());
        assertEquals(Double.valueOf(25.0), result.get().getSollTemperatur());
        assertNull(result.get().getIstTemperatur());
        verify(siemensRepository, times(1)).findTopByTypeOrderByTimestampDesc("SOLL");
    }

    @Test
    void testGetLatestByType_ShouldReturnEmptyWhenNoData() {
        // Arrange
        when(siemensRepository.findTopByTypeOrderByTimestampDesc("IST"))
                .thenReturn(Optional.empty());

        // Act
        Optional<SiemensData> result = siemensService.getLatestByType("IST");

        // Assert
        assertFalse(result.isPresent());
        verify(siemensRepository, times(1)).findTopByTypeOrderByTimestampDesc("IST");
    }

    @Test
    void testGetAllByType_ShouldReturnAllDataOfType() {
        // Arrange
        List<SiemensData> expectedList = Arrays.asList(istData, istData);
        when(siemensRepository.findByTypeOrderByTimestampDesc("IST"))
                .thenReturn(expectedList);

        // Act
        List<SiemensData> result = siemensService.getAllByType("IST");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("IST", result.get(0).getType());
        assertEquals("IST", result.get(1).getType());
        verify(siemensRepository, times(1)).findByTypeOrderByTimestampDesc("IST");
    }
}