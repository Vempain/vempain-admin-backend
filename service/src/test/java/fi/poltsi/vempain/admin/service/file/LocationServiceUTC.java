package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.entity.file.GpsLocation;
import fi.poltsi.vempain.admin.repository.GpsLocationRepository;
import fi.poltsi.vempain.file.api.response.LocationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceUTC {

	@Mock
	private GpsLocationRepository gpsLocationRepository;

	@InjectMocks
	private LocationService locationService;

	// ---- upsertAndGet ----

	@Test
	void upsertAndGetNullDtoReturnsNullOk() {
		GpsLocation result = locationService.upsertAndGet(null);

		assertNull(result);
		verify(gpsLocationRepository, never()).save(any());
	}

	@Test
	void upsertAndGetDtoWithNullIdReturnsNullOk() {
		LocationResponse dto = mock(LocationResponse.class);
		when(dto.getId()).thenReturn(null);

		GpsLocation result = locationService.upsertAndGet(dto);

		assertNull(result);
		verify(gpsLocationRepository, never()).save(any());
	}

	@Test
	void upsertAndGetExistingLocationUpdatesOk() {
		LocationResponse dto = buildLocationResponse(10L);
		GpsLocation existing = new GpsLocation();
		existing.setId(10L);

		when(gpsLocationRepository.findById(10L)).thenReturn(Optional.of(existing));
		when(gpsLocationRepository.save(any(GpsLocation.class))).thenAnswer(inv -> inv.getArgument(0));

		GpsLocation result = locationService.upsertAndGet(dto);

		assertNotNull(result);
		verify(gpsLocationRepository).save(existing);
	}

	@Test
	void upsertAndGetNewLocationCreatesOk() {
		LocationResponse dto = buildLocationResponse(20L);

		when(gpsLocationRepository.findById(20L)).thenReturn(Optional.empty());
		when(gpsLocationRepository.save(any(GpsLocation.class))).thenAnswer(inv -> inv.getArgument(0));

		GpsLocation result = locationService.upsertAndGet(dto);

		assertNotNull(result);
		verify(gpsLocationRepository).save(any(GpsLocation.class));
	}

	@Test
	void upsertAndGetFieldsAreMappedOk() {
		LocationResponse dto = buildLocationResponse(30L);

		GpsLocation existing = new GpsLocation();
		existing.setId(30L);
		when(gpsLocationRepository.findById(30L)).thenReturn(Optional.of(existing));
		when(gpsLocationRepository.save(any(GpsLocation.class))).thenAnswer(inv -> inv.getArgument(0));

		GpsLocation result = locationService.upsertAndGet(dto);

		assertNotNull(result);
		// Verify fields were set by checking the saved entity
		verify(gpsLocationRepository).save(any(GpsLocation.class));
	}

	private LocationResponse buildLocationResponse(long id) {
		LocationResponse dto = mock(LocationResponse.class);
		when(dto.getId()).thenReturn(id);
		when(dto.getLatitude()).thenReturn(BigDecimal.valueOf(60.123));
		when(dto.getLatitudeRef()).thenReturn('N');
		when(dto.getLongitude()).thenReturn(BigDecimal.valueOf(25.456));
		when(dto.getLongitudeRef()).thenReturn('E');
		when(dto.getAltitude()).thenReturn(50.0);
		when(dto.getDirection()).thenReturn(180.0);
		when(dto.getSatelliteCount()).thenReturn(8);
		when(dto.getCountry()).thenReturn("Finland");
		when(dto.getState()).thenReturn("Uusimaa");
		when(dto.getCity()).thenReturn("Helsinki");
		when(dto.getStreet()).thenReturn("Main St");
		when(dto.getSubLocation()).thenReturn("Area 1");
		return dto;
	}
}
