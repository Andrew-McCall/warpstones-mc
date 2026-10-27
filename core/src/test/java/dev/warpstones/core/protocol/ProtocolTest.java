package dev.warpstones.core.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.arbitraries.StringArbitrary;
import org.junit.jupiter.api.Test;

class ProtocolTest {
    @Property
    void signedVarIntRoundTrips(@ForAll int value) throws ProtocolException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Wire.writeSignedVarInt(out, value);
        assertThat(Wire.decode(out.toByteArray(), Wire::readSignedVarInt)).isEqualTo(value);
    }

    @Property
    void configSyncRoundTrips(@ForAll("syncValues") Map<String, Object> values) throws ProtocolException {
        ConfigSyncMessage message = new ConfigSyncMessage(values);
        assertThat(ConfigSyncMessage.decode(message.encode())).isEqualTo(message);
    }

    @Property
    void arbitraryBytesAreDecodedOrRejected(@ForAll byte[] bytes) {
        try {
            ConfigSyncMessage.decode(bytes);
        } catch (ProtocolException expected) {
            // Rejection is the only acceptable failure.
        }
    }

    @Test
    void truncatedAndTrailingBytesAreRejected() {
        byte[] encoded = new ConfigSyncMessage(Map.of("access.nameMaxLength", 32)).encode();
        assertThatThrownBy(() -> ConfigSyncMessage.decode(Arrays.copyOf(encoded, encoded.length - 1)))
                .isInstanceOf(ProtocolException.class);
        assertThatThrownBy(() -> ConfigSyncMessage.decode(Arrays.copyOf(encoded, encoded.length + 1)))
                .isInstanceOf(ProtocolException.class);
    }

    @Test
    void oversizedTextIsRejectedBeforeAllocation() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Wire.writeVarInt(out, 1);
        Wire.writeVarInt(out, Integer.MAX_VALUE);
        assertThatThrownBy(() -> ConfigSyncMessage.decode(out.toByteArray()))
                .isInstanceOf(ProtocolException.class).hasMessageContaining("size");
    }

    @Test
    void overlongVarIntIsRejected() {
        byte[] sixBytes = {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0};
        assertThatThrownBy(() -> Wire.decode(sixBytes, Wire::readVarInt)).isInstanceOf(ProtocolException.class);
    }

    @Provide
    Arbitrary<Map<String, Object>> syncValues() {
        StringArbitrary text = Arbitraries.strings().withCharRange('\u0000', '퟿');
        Arbitrary<Object> value = Arbitraries.oneOf(
                Arbitraries.of(true, false).map(Object.class::cast),
                Arbitraries.integers().map(Object.class::cast),
                Arbitraries.doubles().map(Object.class::cast),
                text.ofMaxLength(1024).map(Object.class::cast));
        return Arbitraries.maps(text.ofMinLength(1).ofMaxLength(128), value).ofMaxSize(256);
    }
}
