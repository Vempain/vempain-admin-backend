package fi.poltsi.vempain.tools;

import java.time.Instant;

/**
 * Release state of a resource that is published to the website: the one rule behind the "modified after publish" filter of the page, gallery and site file
 * listings, so that every list answers the same question.
 */
public final class ReleaseState {

    private ReleaseState() {}

    /**
     * @param published when the resource was last released to the website, null when never
     * @param modified  when the resource was last modified, null when never modified since its creation
     * @return {@code true} when the resource was both released and modified, and the release is older than the modification, i.e. the website shows an outdated
     *         copy
     */
    public static boolean isModifiedAfterPublish(Instant published, Instant modified) {
        return published != null
               && modified != null
               && published.isBefore(modified);
    }
}
