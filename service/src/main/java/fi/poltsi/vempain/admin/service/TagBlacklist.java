package fi.poltsi.vempain.admin.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.poltsi.vempain.admin.entity.Subject;
import lombok.extern.slf4j.Slf4j;

/**
 * An immutable snapshot of the tag blacklist, taken once per publish and applied to every resource of it. A tag matches when its trimmed name equals a
 * blacklisted name regardless of case; a hierarchical tag ({@code Europe|Finland|Helsinki}) matches when any of its segments does.
 */
@Slf4j
public record TagBlacklist(Set<String> names) {

    public static final TagBlacklist EMPTY = new TagBlacklist(Set.of());

    /** The keyword-like keys of the metadata JSON whose values are tags, in any group of the JSON and in any case. */
    static final Set<String> TAG_KEYS = Set.of("keywords", "subject", "hierarchicalsubject", "weightedflatsubject", "tagslist", "lastkeywordxmp",
            "lastkeywordiptc", "category", "supplementalcategories");

    public TagBlacklist {
        names = names == null ? Set.of()
                : names.stream()
                    .filter(name -> name != null
                                    && !name.isBlank())
                    .map(TagBlacklist::normalize)
                    .collect(Collectors.toUnmodifiableSet());
    }

    public static TagBlacklist of(List<String> names) {
        return new TagBlacklist(names == null ? Set.of()
                : Set.copyOf(names.stream()
                    .filter(name -> name != null)
                    .toList()));
    }

    public boolean isEmpty() {
        return names.isEmpty();
    }

    /**
     * @return whether the tag, or any segment of a hierarchical tag, is blacklisted
     */
    public boolean isBlacklisted(String tag) {
        if (tag == null
            || names.isEmpty()) {
            return false;
        }
        if (names.contains(normalize(tag))) {
            return true;
        }
        if (tag.indexOf('|') >= 0) {
            for (var segment : tag.split("\\|")) {
                if (names.contains(normalize(segment))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The subjects that may be published: every blacklisted one is left out (matched on the subject name).
     */
    public List<Subject> filterSubjects(List<Subject> subjects) {
        if (subjects == null
            || names.isEmpty()) {
            return subjects;
        }
        return subjects.stream()
            .filter(subject -> !isBlacklisted(subject.getSubjectName()))
            .toList();
    }

    /**
     * The metadata JSON of a file with the blacklisted tags removed from its keyword lists ({@code Keywords}, {@code Subject}, {@code HierarchicalSubject}, ...
     * in any group). A single blacklisted value removes the key; a list keeps its other values. The JSON is returned unchanged when the blacklist is empty, the
     * text is not JSON or nothing had to be removed.
     */
    public String stripMetadata(String metadataJson) {
        if (metadataJson == null
            || metadataJson.isBlank()
            || names.isEmpty()) {
            return metadataJson;
        }
        try {
            var trimmed = metadataJson.trim();
            if (trimmed.startsWith("[")) {
                var array = new JSONArray(trimmed);
                var changed = false;
                for (int i = 0; i < array.length(); i++) {
                    var item = array.opt(i);
                    if (item instanceof JSONObject object) {
                        changed |= strip(object);
                    }
                }
                return changed ? array.toString() : metadataJson;
            }
            var object = new JSONObject(trimmed);
            return strip(object) ? object.toString() : metadataJson;
        } catch (JSONException e) {
            log.warn("The metadata is not JSON, publishing it unchanged: {}", e.getMessage());
            return metadataJson;
        }
    }

    private boolean strip(JSONObject object) {
        var changed = false;
        for (var key : List.copyOf(object.keySet())) {
            var value = object.opt(key);
            if (value instanceof JSONObject nested) {
                changed |= strip(nested);
                continue;
            }
            if (!TAG_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (value instanceof JSONArray array) {
                var kept = new JSONArray();
                for (var item : array) {
                    if (item instanceof String text
                        && isBlacklisted(text)) {
                        changed = true;
                    } else {
                        kept.put(item);
                    }
                }
                if (kept.length() != array.length()) {
                    if (kept.isEmpty()) {
                        object.remove(key);
                    } else {
                        object.put(key, kept);
                    }
                }
            } else if (value instanceof String text
                       && isBlacklisted(text)) {
                object.remove(key);
                changed = true;
            }
        }
        return changed;
    }

    private static String normalize(String name) {
        return name.trim()
            .toLowerCase(Locale.ROOT);
    }
}
