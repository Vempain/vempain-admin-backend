package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.entity.file.GpsLocation;
import fi.poltsi.vempain.admin.repository.GpsLocationRepository;
import fi.poltsi.vempain.file.api.response.LocationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationService {

	private final GpsLocationRepository gpsLocationRepository;

	@Transactional
	public GpsLocation upsertAndGet(LocationResponse dto) {
		if (dto == null || dto.getId() == null) {
			return null;
		}
		var entity = gpsLocationRepository.findById(dto.getId())
										  .orElseGet(() -> {
											  var created = new GpsLocation();
											  created.setId(dto.getId());
											  return created;
										  });

		entity.setLatitude(dto.getLatitude());
		entity.setLatitudeRef(dto.getLatitudeRef());
		entity.setLongitude(dto.getLongitude());
		entity.setLongitudeRef(dto.getLongitudeRef());
		entity.setAltitude(dto.getAltitude());
		entity.setDirection(dto.getDirection());
		entity.setSatelliteCount(dto.getSatelliteCount());
		entity.setCountry(dto.getCountry());
		entity.setState(dto.getState());
		entity.setCity(dto.getCity());
		entity.setStreet(dto.getStreet());
		entity.setSubLocation(dto.getSubLocation());

		return gpsLocationRepository.save(entity);
	}
}

