package org.metalib.papifly.fx.settings.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicFileWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesNewFileAtomically() throws Exception {
        Path target = tempDir.resolve("settings.json");

        AtomicFileWriter.writeAtomically(target, "{\"key\": \"value\"}");

        assertTrue(Files.exists(target));
        assertEquals("{\"key\": \"value\"}", Files.readString(target, StandardCharsets.UTF_8));
        assertFalse(Files.exists(target.resolveSibling("settings.json.bak")),
            "No backup should exist for a brand-new file");
        assertFalse(Files.exists(target.resolveSibling("settings.json.tmp")),
            "Temp file should be cleaned up after write");
    }

    @Test
    void createsBackupOnOverwrite() throws Exception {
        Path target = tempDir.resolve("settings.json");
        Files.writeString(target, "{\"old\": true}", StandardCharsets.UTF_8);

        AtomicFileWriter.writeAtomically(target, "{\"new\": true}");

        assertEquals("{\"new\": true}", Files.readString(target, StandardCharsets.UTF_8));
        Path backup = target.resolveSibling("settings.json.bak");
        assertTrue(Files.exists(backup), "Backup should exist after overwrite");
        assertEquals("{\"old\": true}", Files.readString(backup, StandardCharsets.UTF_8));
    }

    @Test
    void createsParentDirectories() throws Exception {
        Path target = tempDir.resolve("deep/nested/dir/settings.json");

        AtomicFileWriter.writeAtomically(target, "content");

        assertTrue(Files.exists(target));
        assertEquals("content", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void fallsBackWhenAtomicMoveIsNotSupported() throws Exception {
        Path target = tempDir.resolve("settings.json");

        AtomicFileWriter.writeAtomically(target, "{\"key\": \"value\"}", (source, destination, atomic) -> {
            if (atomic) {
                throw new AtomicMoveNotSupportedException(source.toString(), destination.toString(), "unsupported");
            }
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        });

        assertTrue(Files.exists(target));
        assertEquals("{\"key\": \"value\"}", Files.readString(target, StandardCharsets.UTF_8));
        assertFalse(Files.exists(target.resolveSibling("settings.json.tmp")),
            "Temp file should be cleaned up after fallback move");
    }

    @Test
    void cleansUpTempFileWhenMoveFails() throws Exception {
        Path target = tempDir.resolve("settings.json");
        Files.writeString(target, "{\"old\": true}", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> AtomicFileWriter.writeAtomically(target, "{\"new\": true}",
            (source, destination, atomic) -> {
                throw new IOException("rename failed");
            }));

        assertFalse(Files.exists(target.resolveSibling("settings.json.tmp")),
            "Temp file should be deleted when rename fails");
        assertEquals("{\"old\": true}", Files.readString(target, StandardCharsets.UTF_8));
        assertTrue(Files.exists(target.resolveSibling("settings.json.bak")),
            "Backup should still exist after a failed overwrite");
    }
}
