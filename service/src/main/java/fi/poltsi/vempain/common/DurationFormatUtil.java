package fi.poltsi.vempain.common;

import java.time.Duration;

public final class DurationFormatUtil {

	private DurationFormatUtil() {
	}

	public static String toHhMmSs(Duration duration) {
		if (duration == null) {
			return null;
		}
		long seconds = Math.abs(duration.getSeconds());
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long secs = seconds % 60;
		String formatted = String.format("%d:%02d:%02d", hours, minutes, secs);
		return duration.isNegative() ? "-" + formatted : formatted;
	}
}
