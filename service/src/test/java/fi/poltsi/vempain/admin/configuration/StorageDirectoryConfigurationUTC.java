package fi.poltsi.vempain.admin.configuration;

import java.io.File;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import fi.poltsi.vempain.common.api.FileTypeEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class StorageDirectoryConfigurationUTC {

    @TempDir
    Path tempDir;

    private StorageDirectoryConfiguration configurationFor(Path storage) {
        var configuration = new StorageDirectoryConfiguration();
        ReflectionTestUtils.setField(configuration, "siteFileStorage", storage.toString());
        return configuration;
    }

    @Test
    void everyFileTypeGetsItsOwnDirectoryUnderTheStorage() {
        var storage = tempDir.resolve("storage");

        var locations = configurationFor(storage).storageLocations();

        assertEquals(FileTypeEnum.getFileTypeNames(), locations.keySet());
        for (var name : FileTypeEnum.getFileTypeNames()) {
            assertEquals(storage + File.separator + name, locations.get(name));
            assertTrue(Files.isDirectory(storage.resolve(name)), name);
        }
    }

    @Test
    void aStorageThatCannotBeCreatedFailsTheStartup() throws Exception {
        var regularFile = tempDir.resolve("regular.txt");
        Files.writeString(regularFile, "not a directory");

        assertThrows(FileSystemNotFoundException.class, () -> configurationFor(regularFile).storageLocations());
    }

    @Test
    void anUnreadableStorageDirectoryFailsTheStartup() throws Exception {
        assumeTrue(Files.getFileAttributeView(tempDir, java.nio.file.attribute.PosixFileAttributeView.class) != null, "POSIX permissions required");
        assumeFalse("root".equals(System.getProperty("user.name")), "root ignores directory permissions");
        var storage = tempDir.resolve("storage");
        var unreadable = new java.util.ArrayList<Path>();

        for (var name : FileTypeEnum.getFileTypeNames()) {
            var directory = Files.createDirectories(storage.resolve(name));
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("---------"));
            unreadable.add(directory);
        }

        try {
            assertThrows(FileSystemNotFoundException.class, () -> configurationFor(storage).storageLocations());
        } finally {
            for (var directory : unreadable) {
                Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwxr-xr-x"));
            }
        }
    }
}
