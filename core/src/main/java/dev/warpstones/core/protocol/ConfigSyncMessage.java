package dev.warpstones.core.protocol;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** Server → client: the values of every synced server config key. */
public record ConfigSyncMessage(Map<String, Object> values) {
    private static final int MAX_ENTRIES = 256;
    private static final int MAX_KEY_LENGTH = 128;
    private static final int MAX_TEXT_LENGTH = 1024;
    private static final int BOOLEAN = 0;
    private static final int INTEGER = 1;
    private static final int DOUBLE = 2;
    private static final int TEXT = 3;

    public ConfigSyncMessage {
        if (values.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("more than " + MAX_ENTRIES + " synced values");
        }
        values = Map.copyOf(values);
    }

    public byte[] encode() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Wire.writeVarInt(out, values.size());
        for (Map.Entry<String, Object> e : values.entrySet()) {
            Wire.writeString(out, e.getKey(), MAX_KEY_LENGTH);
            Object value = e.getValue();
            if (value instanceof Boolean b) {
                out.write(BOOLEAN);
                Wire.writeBoolean(out, b);
            } else if (value instanceof Integer i) {
                out.write(INTEGER);
                Wire.writeSignedVarInt(out, i);
            } else if (value instanceof Double d) {
                out.write(DOUBLE);
                Wire.writeDouble(out, d);
            } else if (value instanceof String s) {
                out.write(TEXT);
                Wire.writeString(out, s, MAX_TEXT_LENGTH);
            } else {
                throw new IllegalArgumentException("can't sync " + value.getClass().getName());
            }
        }
        return out.toByteArray();
    }

    public static ConfigSyncMessage decode(byte[] bytes) throws ProtocolException {
        return Wire.decode(bytes, in -> {
            int count = Wire.readSize(in, MAX_ENTRIES);
            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                String key = Wire.readString(in, MAX_KEY_LENGTH);
                int tag = in.get();
                Object value = switch (tag) {
                    case BOOLEAN -> Wire.readBoolean(in);
                    case INTEGER -> Wire.readSignedVarInt(in);
                    case DOUBLE -> Wire.readDouble(in);
                    case TEXT -> Wire.readString(in, MAX_TEXT_LENGTH);
                    default -> throw new ProtocolException("unknown value type " + tag);
                };
                if (values.put(key, value) != null) {
                    throw new ProtocolException("duplicate key " + key);
                }
            }
            return new ConfigSyncMessage(values);
        });
    }
}
