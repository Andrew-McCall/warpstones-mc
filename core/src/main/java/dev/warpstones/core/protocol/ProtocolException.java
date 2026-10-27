package dev.warpstones.core.protocol;

/** Malformed or oversized network data. Decoders throw only this. */
public final class ProtocolException extends Exception {
    private static final long serialVersionUID = 1L;

    public ProtocolException(String message) {
        super(message);
    }
}
