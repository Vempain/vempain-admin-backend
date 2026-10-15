package fi.poltsi.vempain.admin.entity.file;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GpsLocationUTC {

    private static GpsLocation sample() {
        return GpsLocation.builder()
            .id(42L)
            .latitude(new BigDecimal("60.16952"))
            .latitudeRef('N')
            .longitude(new BigDecimal("24.93545"))
            .longitudeRef('E')
            .altitude(12.5)
            .direction(180.0)
            .satelliteCount(9)
            .country("Finland")
            .state("Uusimaa")
            .city("Helsinki")
            .street("Mannerheimintie")
            .subLocation("Centre")
            .build();
    }

    @Test
    void toResponseCopiesEveryField() {
        var response = sample().toResponse();

        assertEquals(42L, response.getId());
        assertEquals(new BigDecimal("60.16952"), response.getLatitude());
        assertEquals('N', response.getLatitudeRef());
        assertEquals(new BigDecimal("24.93545"), response.getLongitude());
        assertEquals('E', response.getLongitudeRef());
        assertEquals(12.5, response.getAltitude());
        assertEquals(180.0, response.getDirection());
        assertEquals(9, response.getSatelliteCount());
        assertEquals("Finland", response.getCountry());
        assertEquals("Uusimaa", response.getState());
        assertEquals("Helsinki", response.getCity());
        assertEquals("Mannerheimintie", response.getStreet());
        assertEquals("Centre", response.getSubLocation());
    }

    @Test
    void toWebGpsLocationCopiesEveryFieldIncludingTheUpstreamId() {
        var web = sample().toWebGpsLocation();

        assertEquals(42L, web.getId());
        assertEquals(new BigDecimal("60.16952"), web.getLatitude());
        assertEquals('N', web.getLatitudeRef());
        assertEquals(new BigDecimal("24.93545"), web.getLongitude());
        assertEquals('E', web.getLongitudeRef());
        assertEquals(12.5, web.getAltitude());
        assertEquals(180.0, web.getDirection());
        assertEquals(9, web.getSatelliteCount());
        assertEquals("Finland", web.getCountry());
        assertEquals("Uusimaa", web.getState());
        assertEquals("Helsinki", web.getCity());
        assertEquals("Mannerheimintie", web.getStreet());
        assertEquals("Centre", web.getSubLocation());
    }

    @Test
    void optionalFieldsStayNull() {
        var bare = GpsLocation.builder()
            .id(1L)
            .latitude(BigDecimal.ONE)
            .latitudeRef('N')
            .longitude(BigDecimal.TEN)
            .longitudeRef('E')
            .build();

        assertNull(bare.toResponse()
            .getAltitude());
        assertNull(bare.toWebGpsLocation()
            .getCity());
    }
}
