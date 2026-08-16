package fi.poltsi.vempain.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DurationFormatUtilUTC {
	@Test
	void formatsNullPositiveAndNegativeDurations() {
		assertNull(DurationFormatUtil.toHhMmSs(null));
		assertEquals("1:01:02", DurationFormatUtil.toHhMmSs(Duration.ofSeconds(3662)));
		assertEquals("-1:01:02", DurationFormatUtil.toHhMmSs(Duration.ofSeconds(-3662)));
		assertEquals("0:00:00", DurationFormatUtil.toHhMmSs(Duration.ZERO));
	}
}
