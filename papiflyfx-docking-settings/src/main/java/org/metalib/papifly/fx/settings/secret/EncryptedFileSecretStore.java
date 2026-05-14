package org.metalib.papifly.fx.settings.secret;

import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.internal.AtomicFileWriter;
import org.metalib.papifly.fx.settings.internal.SettingsJsonCodec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EncryptedFileSecretStore implements SecretStore {

    private static final Logger LOG = Logger.getLogger(EncryptedFileSecretStore.class.getName());

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int ITERATIONS = 65_536;

    private final Path secretsFile;
    private final SettingsJsonCodec codec = new SettingsJsonCodec();
    private final SecureRandom random = new SecureRandom();

    public EncryptedFileSecretStore() {
        this(Path.of(System.getProperty("user.home"), ".papiflyfx", "secrets.enc"));
    }

    public EncryptedFileSecretStore(Path secretsFile) {
        this.secretsFile = secretsFile;
    }

    @Override
    public synchronized Optional<String> getSecret(String key) {
        return Optional.ofNullable(loadSecrets().get(key)).filter(value -> !value.isBlank());
    }

    @Override
    public synchronized void setSecret(String key, String value) {
        Map<String, String> secrets = loadSecrets();
        if (value == null || value.isBlank()) {
            secrets.remove(key);
        } else {
            secrets.put(key, value);
        }
        saveSecrets(secrets);
    }

    @Override
    public synchronized void clearSecret(String key) {
        Map<String, String> secrets = loadSecrets();
        if (secrets.remove(key) != null) {
            saveSecrets(secrets);
        }
    }

    @Override
    public synchronized Set<String> listKeys() {
        return new LinkedHashSet<>(loadSecrets().keySet());
    }

    @Override
    public String backendName() {
        return "Encrypted File";
    }

    private Map<String, String> loadSecrets() {
        if (!Files.exists(secretsFile)) {
            return new LinkedHashMap<>();
        }
        Path bakFile = backupFile();
        SecretFileReadFailure primaryFailure;
        try {
            return parseSecretsFile(secretsFile);
        } catch (SecretFileReadFailure failure) {
            primaryFailure = failure;
            logRecoveryAttempt(secretsFile, bakFile, failure);
        }

        if (Files.exists(bakFile)) {
            try {
                Map<String, String> recovered = parseSecretsFile(bakFile);
                LOG.info("Recovered secrets from backup " + bakFile);
                return recovered;
            } catch (SecretFileReadFailure backupFailure) {
                if (primaryFailure.unrecoverable() || backupFailure.unrecoverable()) {
                    throw unrecoverableLoadFailure(primaryFailure, backupFailure);
                }
                LOG.log(Level.WARNING, "Backup file " + bakFile + " is also malformed, resetting to empty",
                    backupFailure.getCause());
            }
        } else if (primaryFailure.unrecoverable()) {
            throw unrecoverableLoadFailure(primaryFailure, null);
        } else {
            LOG.warning("No backup file found at " + bakFile + ", resetting to empty");
        }
        return new LinkedHashMap<>();
    }

    private Map<String, String> parseSecretsFile(Path path) throws SecretFileReadFailure {
        String json;
        try {
            json = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new SecretFileReadFailure(path, SecretFileFailureKind.MALFORMED, exception);
        }

        Map<String, Object> envelope = parseJson(path, json);
        byte[] salt = decode(path, envelope, "salt");
        byte[] iv = decode(path, envelope, "iv");
        byte[] encrypted = decode(path, envelope, "payload");
        byte[] decrypted = decrypt(path, encrypted, salt, iv);
        try {
            Map<String, Object> payload = parseJson(path, new String(decrypted, StandardCharsets.UTF_8));
            Map<String, String> secrets = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                if (entry.getValue() != null) {
                    secrets.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            return secrets;
        } finally {
            Arrays.fill(decrypted, (byte) 0);
        }
    }

    private void saveSecrets(Map<String, String> secrets) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.putAll(secrets);
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(salt);
        random.nextBytes(iv);
        byte[] plaintext = codec.toJson(new LinkedHashMap<>(payload)).getBytes(StandardCharsets.UTF_8);
        try {
            byte[] encrypted = encrypt(plaintext, salt, iv);
            try {
                Map<String, Object> envelope = new LinkedHashMap<>();
                envelope.put("version", 1);
                envelope.put("salt", Base64.getEncoder().encodeToString(salt));
                envelope.put("iv", Base64.getEncoder().encodeToString(iv));
                envelope.put("payload", Base64.getEncoder().encodeToString(encrypted));
                AtomicFileWriter.writeAtomically(secretsFile, codec.toJson(envelope));
            } finally {
                Arrays.fill(encrypted, (byte) 0);
            }
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to write encrypted secrets to " + secretsFile, exception);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    private byte[] encrypt(byte[] plaintext, byte[] salt, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(salt), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(plaintext);
    }

    private byte[] decrypt(byte[] encrypted, byte[] salt, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(salt), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(encrypted);
    }

    private byte[] decrypt(Path path, byte[] encrypted, byte[] salt, byte[] iv) throws SecretFileReadFailure {
        try {
            return decrypt(encrypted, salt, iv);
        } catch (GeneralSecurityException exception) {
            throw new SecretFileReadFailure(path, SecretFileFailureKind.UNDECRYPTABLE, exception);
        }
    }

    private SecretKey deriveKey(byte[] salt) throws GeneralSecurityException {
        char[] seed = machineSeed().toCharArray();
        try {
            PBEKeySpec spec = new PBEKeySpec(seed, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF);
            byte[] encoded = factory.generateSecret(spec).getEncoded();
            try {
                return new SecretKeySpec(encoded, "AES");
            } finally {
                Arrays.fill(encoded, (byte) 0);
            }
        } finally {
            Arrays.fill(seed, '\0');
        }
    }

    private String machineSeed() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return System.getProperty("user.name", "")
                + '|'
                + System.getProperty("os.name", "")
                + '|'
                + System.getProperty("os.arch", "")
                + '|'
                + host;
        } catch (IOException exception) {
            return System.getProperty("user.name", "")
                + '|'
                + System.getProperty("os.name", "")
                + '|'
                + System.getProperty("os.arch", "");
        }
    }

    private Map<String, Object> parseJson(Path path, String json) throws SecretFileReadFailure {
        try {
            return codec.fromJson(json);
        } catch (RuntimeException exception) {
            throw new SecretFileReadFailure(path, SecretFileFailureKind.MALFORMED, exception);
        }
    }

    private byte[] decode(Path path, Map<String, Object> envelope, String key) throws SecretFileReadFailure {
        Object value = envelope.get(key);
        if (value == null) {
            throw new SecretFileReadFailure(path, SecretFileFailureKind.MALFORMED,
                new IllegalStateException("Missing envelope field '" + key + "'"));
        }
        try {
            return Base64.getDecoder().decode(String.valueOf(value));
        } catch (IllegalArgumentException exception) {
            throw new SecretFileReadFailure(path, SecretFileFailureKind.MALFORMED, exception);
        }
    }

    private Path backupFile() {
        return secretsFile.resolveSibling(secretsFile.getFileName() + ".bak");
    }

    private void logRecoveryAttempt(Path path, Path backupFile, SecretFileReadFailure failure) {
        Level level = failure.unrecoverable() ? Level.SEVERE : Level.WARNING;
        String detail = failure.unrecoverable()
            ? "Unable to decrypt/authenticate secrets file "
            : "Malformed secrets file ";
        LOG.log(level, detail + path + ", attempting .bak recovery from " + backupFile, failure.getCause());
    }

    private IllegalStateException unrecoverableLoadFailure(
        SecretFileReadFailure primaryFailure,
        SecretFileReadFailure backupFailure
    ) {
        SecretFileReadFailure lead = primaryFailure != null && primaryFailure.unrecoverable()
            ? primaryFailure
            : backupFailure;
        IllegalStateException exception = new IllegalStateException(
            "Unable to load encrypted secrets from " + secretsFile
                + "; the persisted secret store could not be decrypted or authenticated",
            lead == null ? null : lead.getCause()
        );
        addSuppressedCause(exception, primaryFailure, lead);
        addSuppressedCause(exception, backupFailure, lead);
        return exception;
    }

    private void addSuppressedCause(
        IllegalStateException target,
        SecretFileReadFailure failure,
        SecretFileReadFailure lead
    ) {
        if (failure == null || failure == lead || failure.getCause() == null) {
            return;
        }
        target.addSuppressed(failure.getCause());
    }

    private enum SecretFileFailureKind {
        MALFORMED,
        UNDECRYPTABLE
    }

    private static final class SecretFileReadFailure extends Exception {

        private final SecretFileFailureKind kind;

        private SecretFileReadFailure(Path path, SecretFileFailureKind kind, Throwable cause) {
            super(cause);
            this.kind = kind;
        }

        private boolean unrecoverable() {
            return kind == SecretFileFailureKind.UNDECRYPTABLE;
        }
    }
}
