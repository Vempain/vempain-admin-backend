package fi.poltsi.vempain.admin.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.admin.entity.Subject;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The blacklist snapshot matches tags regardless of case and spacing, including the segments of hierarchical tags, and removes them from subjects and from the
 * keyword lists of the metadata JSON without touching anything else.
 */
class TagBlacklistUTC {

    private final TagBlacklist blacklist = TagBlacklist.of(Arrays.asList(" Private ", "Family", null));

    private static Subject subject(long id, String name) {
        return Subject.builder()
            .id(id)
            .subjectName(name)
            .build();
    }

    @Test
    void matchesCaseInsensitivelyAndInHierarchies() {
        assertThat(blacklist.names()).containsExactlyInAnyOrder("private", "family");
        assertThat(blacklist.isBlacklisted("PRIVATE")).isTrue();
        assertThat(blacklist.isBlacklisted(" family ")).isTrue();
        assertThat(blacklist.isBlacklisted("People|Family|Kids")).isTrue();
        assertThat(blacklist.isBlacklisted("Privately")).isFalse();
        assertThat(blacklist.isBlacklisted(null)).isFalse();
        assertThat(TagBlacklist.EMPTY.isBlacklisted("private")).isFalse();
        assertThat(TagBlacklist.EMPTY.isEmpty()).isTrue();
        assertThat(new TagBlacklist(null).isEmpty()).isTrue();
        assertThat(new TagBlacklist(Set.of(" ")).isEmpty()).isTrue();
    }

    @Test
    void filtersSubjectsByName() {
        var subjects = List.of(subject(1, "sea"), subject(2, "Private"), subject(3, "people|family"));

        assertThat(blacklist.filterSubjects(subjects)).extracting(Subject::getSubjectName)
            .containsExactly("sea");
        assertThat(TagBlacklist.EMPTY.filterSubjects(subjects)).isSameAs(subjects);
        assertThat(blacklist.filterSubjects(null)).isNull();
    }

    @Test
    void stripsBlacklistedValuesFromEveryKeywordListOfTheMetadata() {
        var metadata = "[{\"Keywords\":[\"sea\",\"Private\"],\"Subject\":\"family\",\"XMP-lr\":{\"HierarchicalSubject\":[\"People|Family\",\"Nature|Sea\"]},"
                + "\"IPTC\":{\"Keywords\":\"private\",\"Caption-Abstract\":\"private moment\"},\"Make\":\"Canon\",\"Artist\":\"Private Person\"}]";

        var stripped = new JSONArray(blacklist.stripMetadata(metadata)).getJSONObject(0);

        assertThat(stripped.getJSONArray("Keywords")
            .toList()).containsExactly("sea");
        assertThat(stripped.has("Subject")).isFalse();
        assertThat(stripped.getJSONObject("XMP-lr")
            .getJSONArray("HierarchicalSubject")
            .toList()).containsExactly("Nature|Sea");
        assertThat(stripped.getJSONObject("IPTC")
            .has("Keywords")).isFalse();
        // only keyword lists are touched, free text and other fields stay
        assertThat(stripped.getJSONObject("IPTC")
            .getString("Caption-Abstract")).isEqualTo("private moment");
        assertThat(stripped.getString("Make")).isEqualTo("Canon");
        assertThat(stripped.getString("Artist")).isEqualTo("Private Person");
    }

    @Test
    void leavesTheMetadataAloneWhenNothingMatchesOrItIsNotJson() {
        var untouched = "[{\"Keywords\":[\"sea\"],\"Make\":\"Canon\"}]";
        assertThat(blacklist.stripMetadata(untouched)).isSameAs(untouched);
        assertThat(blacklist.stripMetadata("{\"Keywords\":\"sea\"}")).isEqualTo("{\"Keywords\":\"sea\"}");
        assertThat(blacklist.stripMetadata("{\"Keywords\":\"private\"}")).isEqualTo("{}");
        assertThat(blacklist.stripMetadata("not json")).isEqualTo("not json");
        assertThat(blacklist.stripMetadata(null)).isNull();
        assertThat(blacklist.stripMetadata(" ")).isEqualTo(" ");
        assertThat(TagBlacklist.EMPTY.stripMetadata("[{\"Keywords\":[\"private\"]}]")).isEqualTo("[{\"Keywords\":[\"private\"]}]");
    }
}
