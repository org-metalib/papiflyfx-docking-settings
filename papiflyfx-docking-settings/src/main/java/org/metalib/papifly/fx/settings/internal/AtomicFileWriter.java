package org.metalib.papifly.fx.settings.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes files atomically using a temp-file-then-rename strategy.
 *
 * <p>Before overwriting the target, the existing file (if any) is copied to a
 * {@code .bak} companion for corruption recovery. The new content is written to a
 * {@code .tmp} sibling and then atomically moved over the target.
 */
public final class AtomicFileWriter {

    private static final Logger LOG = Logger.getLogger(AtomicFileWriter.class.getName());

    private AtomicFileWriter() {}

    /**
     * Writes {@code content} to {@code target} atomically.
     *
     * <ol>
     *   <li>Creates parent directories if needed.</li>
     *   <li>If {@code target} already exists, copies it to {@code target.bak}.</li>
     *   <li>Writes {@code content} to {@code target.tmp}.</li>
     *   <li>Atomically renames {@code target.tmp} to {@code target}.</li>
     * </ol>
     *
     * @throws IOException if the write or rename fails
     */
    public static void writeAtomically(Path target, String content) throws IOException {
        writeAtomically(target, content, AtomicFileWriter::moveFile);
    }

    static void writeAtomically(Path target, String content, MoveOperation moveOperation) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path bakFile = target.resolveSibling(target.getFileName() + ".bak");
        Path tmpFile = target.resolveSibling(target.getFileName() + ".tmp");

        // Back up existing file before overwriting
        if (Files.exists(target)) {
            try {
                Files.copy(target, bakFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to create backup of " + target, e);
                // Continue — atomic write is still safer than direct overwrite
            }
        }

        IOException failure = null;
        try {
            Files.writeString(tmpFile, content, StandardCharsets.UTF_8);
            moveIntoPlace(tmpFile, target, moveOperation);
        } catch (IOException exception) {
            failure = exception;
            throw exception;
        } finally {
            cleanupTempFile(tmpFile, failure);
        }
    }

    private static void moveIntoPlace(Path tmpFile, Path target, MoveOperation moveOperation) throws IOException {
        try {
            moveOperation.move(tmpFile, target, true);
        } catch (AtomicMoveNotSupportedException exception) {
            LOG.log(Level.WARNING, "Atomic move is not supported for " + target + ", retrying with REPLACE_EXISTING",
                exception);
            moveOperation.move(tmpFile, target, false);
        }
    }

    private static void moveFile(Path source, Path target, boolean atomic) throws IOException {
        if (atomic) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return;
        }
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void cleanupTempFile(Path tmpFile, IOException failure) throws IOException {
        try {
            Files.deleteIfExists(tmpFile);
        } catch (IOException cleanupFailure) {
            if (failure != null) {
                failure.addSuppressed(cleanupFailure);
                return;
            }
            throw cleanupFailure;
        }
    }

    @FunctionalInterface
    interface MoveOperation {
        void move(Path source, Path target, boolean atomic) throws IOException;
    }
}
