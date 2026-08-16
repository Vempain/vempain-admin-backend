package fi.poltsi.vempain.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DurationToLongConverterUTC {
	private final DurationToLongConverter converter = new DurationToLongConverter();

	@Test
	void convertsBothDirectionsAndNulls() {
		assertEquals(1234L, converter.convertToDatabaseColumn(Duration.ofMillis(1234)));
		assertEquals(Duration.ofMillis(1234), converter.convertToEntityAttribute(1234L));
		assertNull(converter.convertToDatabaseColumn(null));
		assertNull(converter.convertToEntityAttribute(null));
	}
}
