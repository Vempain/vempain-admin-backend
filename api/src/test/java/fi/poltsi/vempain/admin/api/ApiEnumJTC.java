package fi.poltsi.vempain.admin.api;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.admin.api.site.WebSiteConfigurationTypeEnum;
import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The public enums of the api module are carried as their constant names in JSON; this pins every value so a rename or reorder is a visible contract change.
 */
class ApiEnumJTC {

    private final JsonMapper mapper = JsonMapper.builder()
        .build();

    @Test
    void taskTypesArePinned() throws Exception {
        assertEquals(List.of("PUBLISH_PAGE", "PUBLISH_ALL_PAGES", "PUBLISH_GALLERY", "PUBLISH_ALL_GALLERIES", "PUBLISH_SELECTED_GALLERIES",
                "PUBLISH_SELECTED_FILES", "REFRESH_ALL_GALLERY_FILES", "PUBLISH_DATA_SET"), names(TaskTypeEnum.values()));
        assertRoundTrip(TaskTypeEnum.class, TaskTypeEnum.PUBLISH_SELECTED_GALLERIES);
        assertEquals(TaskTypeEnum.PUBLISH_DATA_SET, TaskTypeEnum.valueOf("PUBLISH_DATA_SET"));
    }

    @Test
    void publishStatusesArePinned() throws Exception {
        assertEquals(List.of("NOT_PUBLISHED", "PROCESSING", "PUBLISHED", "CANCELLED"), names(PublishStatusEnum.values()));
        assertRoundTrip(PublishStatusEnum.class, PublishStatusEnum.CANCELLED);
        assertEquals(PublishStatusEnum.PROCESSING, PublishStatusEnum.valueOf("PROCESSING"));
    }

    @Test
    void publishResultsContentTypesAndQueryDetailsArePinned() throws Exception {
        assertEquals(List.of("OK", "FAIL"), names(PublishResultEnum.values()));
        assertRoundTrip(PublishResultEnum.class, PublishResultEnum.FAIL);
        assertEquals(List.of("PAGE", "COMPONENT", "LAYOUT", "FORM", "GALLERY"), names(ContentTypeEnum.values()));
        assertRoundTrip(ContentTypeEnum.class, ContentTypeEnum.GALLERY);
        assertEquals(List.of("MINIMAL", "UNPOPULATED", "FULL"), names(QueryDetailEnum.values()));
        assertRoundTrip(QueryDetailEnum.class, QueryDetailEnum.MINIMAL);
    }

    @Test
    void webSiteConfigurationTypesArePinned() throws Exception {
        assertEquals(List.of("STRING", "NUMBER", "BOOLEAN", "LIST"), names(WebSiteConfigurationTypeEnum.values()));
        assertRoundTrip(WebSiteConfigurationTypeEnum.class, WebSiteConfigurationTypeEnum.LIST);
        assertEquals(WebSiteConfigurationTypeEnum.BOOLEAN, WebSiteConfigurationTypeEnum.valueOf("BOOLEAN"));
    }

    @Test
    void webSiteResourcesArePinned() throws Exception {
        assertEquals(List.of("PAGE", "GALLERY", "SITE_FILE"), names(WebSiteResourceEnum.values()));
        assertRoundTrip(WebSiteResourceEnum.class, WebSiteResourceEnum.SITE_FILE);
        assertEquals(WebSiteResourceEnum.GALLERY, WebSiteResourceEnum.valueOf("GALLERY"));
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values)
            .map(Enum::name)
            .toList();
    }

    private <E extends Enum<E>> void assertRoundTrip(Class<E> type, E value) throws Exception {
        String json = mapper.writeValueAsString(value);
        assertEquals("\"" + value.name() + "\"", json);
        assertEquals(value, mapper.readValue(json, type));
    }
}
