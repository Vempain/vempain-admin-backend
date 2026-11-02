package fi.poltsi.vempain.admin.api;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.admin.api.response.TagResponse;
import fi.poltsi.vempain.admin.rest.TagAPI;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JSON contract of the tag suggestions of the page editor.
 */
class TagContractJTC {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void pathAndLimitsRemainStable() {
        assertEquals("/content-management/tags", TagAPI.MAIN_PATH);
        assertEquals(20, TagAPI.DEFAULT_LIMIT);
        assertEquals(50, TagAPI.MAX_LIMIT);
    }

    @Test
    void responseIsSnakeCase() throws Exception {
        var response = TagResponse.builder()
            .id(12)
            .tagName("summer")
            .build();
        var tree = objectMapper.readTree(objectMapper.writeValueAsString(response));
        assertEquals(Set.of("id", "tag_name"), new HashSet<>(tree.propertyNames()));
        assertEquals("summer", tree.get("tag_name")
            .asString());
        var parsed = objectMapper.readValue("{\"id\": 3, \"tag_name\": \"winter\"}", TagResponse.class);
        assertEquals(3, parsed.getId());
        assertEquals("winter", parsed.getTagName());
    }
}
