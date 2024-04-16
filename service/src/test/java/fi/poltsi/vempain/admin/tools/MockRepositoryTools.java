package fi.poltsi.vempain.admin.tools;

import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.repository.LayoutRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.Mockito.when;

public class MockRepositoryTools {
    ////////// Acl repository start
    ////////// Acl repository private
    ////////// Acl repository end

    ////////// Component repository start
    ////////// Component repository private
    ////////// Component repository end

    ////////// Form repository start
    ////////// Form repository private
    ////////// Form repository end

    ////////// Layout repository start
    public static void layoutRepositoryFindAllOk(LayoutRepository layoutRepository, long count) {
        ArrayList<Layout> layouts = new ArrayList<>();

        for (long i = 0; i < count; i++) {
            layouts.add(makeLayout(i, "Test layout " + i));
        }

        when(layoutRepository.findAll()).thenReturn(layouts);
    }

    public static void layoutRepositoryfindByIdOk(LayoutRepository layoutRepository, long layoutId) {
        Layout           layout         = makeLayout(layoutId, "Test layout");
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(layout.getId())).thenReturn(optionalLayout);
    }

    public static void layoutRepositoryFindByNameOk(LayoutRepository layoutRepository, String layoutName) {
        Layout layout = makeLayout(1L, layoutName);
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findByLayoutName(layout.getLayoutName())).thenReturn(optionalLayout);
    }

    public static Layout makeLayout(long layoutId, String layoutName) {
        return Layout.builder()
                     .id(layoutId)
                     .layoutName(layoutName)
                     .structure("<!-- structure -->")
                     .locked(false)
                     .aclId(1L)
                     .creator(1L)
                     .created(Instant.now().minus(1, ChronoUnit.HOURS))
                     .modifier(1L)
                     .modified(Instant.now())
                     .build();
    }
    ////////// Form repository private
    ////////// Form repository end

}
