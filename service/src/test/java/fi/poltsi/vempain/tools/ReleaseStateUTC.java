package fi.poltsi.vempain.tools;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseStateUTC {

    private static final Instant EARLIER = Instant.parse("2026-01-05T08:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-06T08:00:00Z");

    @Test
    void modifiedAfterTheReleaseIsOutdated() {
        assertTrue(ReleaseState.isModifiedAfterPublish(EARLIER, LATER));
    }

    @Test
    void releasedAfterTheModificationIsUpToDate() {
        assertFalse(ReleaseState.isModifiedAfterPublish(LATER, EARLIER));
        assertFalse(ReleaseState.isModifiedAfterPublish(EARLIER, EARLIER), "a release at the modification time is not outdated");
    }

    @Test
    void neverReleasedOrNeverModifiedIsNotOutdated() {
        assertFalse(ReleaseState.isModifiedAfterPublish(null, LATER));
        assertFalse(ReleaseState.isModifiedAfterPublish(EARLIER, null));
        assertFalse(ReleaseState.isModifiedAfterPublish(null, null));
    }
}
