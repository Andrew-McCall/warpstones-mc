package dev.warpstones.core.config;

public final class InvalidValueException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidValueException(String message) {
        super(message);
    }
}
