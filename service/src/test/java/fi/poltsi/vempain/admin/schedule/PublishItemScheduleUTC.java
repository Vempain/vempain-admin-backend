package fi.poltsi.vempain.admin.schedule;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.entity.PublishSchedule;
import fi.poltsi.vempain.admin.repository.PublishScheduleRepository;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The scheduler publishes the due items without a user context through the {@code *AsSystem} methods (the privileges were checked when the schedule was
 * created) and moves every item through PROCESSING to PUBLISHED, even when the item no longer exists.
 */
@ExtendWith(MockitoExtension.class)
class PublishItemScheduleUTC {

    @Mock
    private PublishScheduleRepository publishScheduleRepository;
    @Mock
    private PublishService publishService;

    @InjectMocks
    private PublishItemSchedule schedule;

    private static PublishSchedule due(long id, ContentTypeEnum type, long itemId) {
        return PublishSchedule.builder()
            .id(id)
            .publishType(type)
            .publishId(itemId)
            .publishStatus(PublishStatusEnum.NOT_PUBLISHED)
            .publishTime(Instant.now()
                .minusSeconds(60))
            .build();
    }

    @Test
    void nothingHappensWhenSchedulingIsDisabled() {
        ReflectionTestUtils.setField(schedule, "schedulingEnabled", false);

        schedule.publishItems();

        verifyNoInteractions(publishScheduleRepository, publishService);
    }

    @Test
    void nothingIsPublishedWhenNoScheduleIsDue() {
        when(publishScheduleRepository.findAllByPublishStatusEqualsAndPublishTimeBefore(eq(PublishStatusEnum.NOT_PUBLISHED), any())).thenReturn(List.of());

        schedule.publishItems();

        verifyNoInteractions(publishService);
        verify(publishScheduleRepository, never()).updatePublishStatus(anyLong(), any());
    }

    @Test
    void duePagesAndGalleriesArePublishedAsSystemAndMarkedPublished() throws Exception {
        when(publishScheduleRepository.findAllByPublishStatusEqualsAndPublishTimeBefore(eq(PublishStatusEnum.NOT_PUBLISHED), any()))
            .thenReturn(List.of(due(1L, ContentTypeEnum.PAGE, 10L), due(2L, ContentTypeEnum.GALLERY, 20L)));

        schedule.publishItems();

        InOrder order = inOrder(publishScheduleRepository, publishService);
        order.verify(publishScheduleRepository)
            .updatePublishStatus(1L, PublishStatusEnum.PROCESSING);
        order.verify(publishService)
            .publishPageAsSystem(10L);
        order.verify(publishScheduleRepository)
            .updatePublishStatus(1L, PublishStatusEnum.PUBLISHED);
        order.verify(publishScheduleRepository)
            .updatePublishStatus(2L, PublishStatusEnum.PROCESSING);
        order.verify(publishService)
            .publishGalleryAsSystem(20L);
        order.verify(publishScheduleRepository)
            .updatePublishStatus(2L, PublishStatusEnum.PUBLISHED);
    }

    @Test
    void missingItemsAndUnknownTypesAreLoggedAndStillMarkedPublished() throws Exception {
        when(publishScheduleRepository.findAllByPublishStatusEqualsAndPublishTimeBefore(eq(PublishStatusEnum.NOT_PUBLISHED), any()))
            .thenReturn(List.of(due(1L, ContentTypeEnum.PAGE, 10L), due(2L, ContentTypeEnum.GALLERY, 20L), due(3L, ContentTypeEnum.COMPONENT, 30L)));
        doThrow(new VempainEntityNotFoundException("Page not found by id: 10", "page")).when(publishService)
            .publishPageAsSystem(10L);
        doThrow(new VempainEntityNotFoundException()).when(publishService)
            .publishGalleryAsSystem(20L);

        schedule.publishItems();

        verify(publishScheduleRepository).updatePublishStatus(1L, PublishStatusEnum.PUBLISHED);
        verify(publishScheduleRepository).updatePublishStatus(2L, PublishStatusEnum.PUBLISHED);
        verify(publishScheduleRepository).updatePublishStatus(3L, PublishStatusEnum.PUBLISHED);
        verify(publishService, never()).publishPageAsSystem(30L);
        verify(publishService, never()).publishGalleryAsSystem(30L);
    }
}
