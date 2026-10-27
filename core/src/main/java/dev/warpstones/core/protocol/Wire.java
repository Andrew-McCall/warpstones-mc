package dev.warpstones.core.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class Wire {
    @FunctionalInterface
    public interface Reader<T> {
        T read(ByteBuffer in) throws ProtocolException;
    }

    private Wire() {
    }

    /** Decodes a whole message; truncated input and trailing bytes are both rejected. */
    public static <T> T decode(byte[] bytes, Reader<T> reader) throws ProtocolException {
        ByteBuffer in = ByteBuffer.wrap(bytes);
        T value;
        try {
            value = reader.read(in);
        } catch (BufferUnderflowException e) {
            throw new ProtocolException("message ends early");
        }
        if (in.hasRemaining()) {
            throw new ProtocolException(in.remaining() + " unread bytes");
        }
        return value;
    }

    public static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7f) != 0) {
            out.write((value & 0x7f) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    public static int readVarInt(ByteBuffer in) throws ProtocolException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int b = in.get() & 0xff;
            if (shift == 28 && (b & 0xf0) != 0) {
                throw new ProtocolException("varint too large");
            }
            value |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
        }
        throw new ProtocolException("varint too long");
    }

    public static void writeSignedVarInt(ByteArrayOutputStream out, int value) {
        writeVarInt(out, (value << 1) ^ (value >> 31));
    }

    public static int readSignedVarInt(ByteBuffer in) throws ProtocolException {
        int zigzag = readVarInt(in);
        return (zigzag >>> 1) ^ -(zigzag & 1);
    }

    public static int readSize(ByteBuffer in, int max) throws ProtocolException {
        int size = readVarInt(in);
        if (size < 0 || size > max) {
            throw new ProtocolException("size " + size + " is outside 0.." + max);
        }
        return size;
    }

    public static void writeBoolean(ByteArrayOutputStream out, boolean value) {
        out.write(value ? 1 : 0);
    }

    public static boolean readBoolean(ByteBuffer in) throws ProtocolException {
        byte b = in.get();
        if (b != 0 && b != 1) {
            throw new ProtocolException("bad boolean " + b);
        }
        return b == 1;
    }

    public static void writeDouble(ByteArrayOutputStream out, double value) {
        out.writeBytes(ByteBuffer.allocate(Double.BYTES).putDouble(value).array());
    }

    public static double readDouble(ByteBuffer in) {
        return in.getDouble();
    }

    public static void writeString(ByteArrayOutputStream out, String value, int maxChars) {
        if (value.length() > maxChars) {
            throw new IllegalArgumentException("text longer than " + maxChars + " characters");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.writeBytes(bytes);
    }

    public static String readString(ByteBuffer in, int maxChars) throws ProtocolException {
        // A UTF-16 char never takes more than 3 UTF-8 bytes, which bounds the allocation.
        byte[] bytes = new byte[readSize(in, maxChars * 3)];
        in.get(bytes);
        String value;
        try {
            value = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new ProtocolException("text is not valid UTF-8");
        }
        if (value.length() > maxChars) {
            throw new ProtocolException("text longer than " + maxChars + " characters");
        }
        return value;
    }
}
