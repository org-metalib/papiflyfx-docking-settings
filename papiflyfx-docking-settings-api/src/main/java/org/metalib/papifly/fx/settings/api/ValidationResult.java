package org.metalib.papifly.fx.settings.api;

public record ValidationResult(Level level, String message) {

    public static final ValidationResult OK = new ValidationResult(Level.OK, "");

    public enum Level {
        OK,
        INFO,
        WARNING,
        ERROR
    }

    public static ValidationResult info(String message) {
        return new ValidationResult(Level.INFO, message);
    }

    public static ValidationResult warning(String message) {
        return new ValidationResult(Level.WARNING, message);
    }

    public static ValidationResult error(String message) {
        return new ValidationResult(Level.ERROR, message);
    }

    public boolean isValid() {
        return level != Level.ERROR;
    }
}
