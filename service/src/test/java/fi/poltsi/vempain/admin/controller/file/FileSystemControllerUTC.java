package fi.poltsi.vempain.admin.controller.file;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.poltsi.vempain.admin.api.response.file.DirectoryNodeResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.file.FileSystemService;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileSystemControllerUTC {

    @Mock
    private FileSystemService fileSystemService;
    @Mock
    private AccessService accessService;
    @InjectMocks
    private FileSystemController controller;

    @Test
    void returnsConvertedDirectoryTree() {
        List<DirectoryNodeResponse> tree = List.of(DirectoryNodeResponse.builder()
            .build());
        when(fileSystemService.getConvertedDirectoryTree()).thenReturn(tree);
        assertSame(tree, controller.getConvertedDirectoryStructure()
            .getBody());
    }
}
