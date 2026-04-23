package gt.agromis.shared.eventing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;

/** Serializa y deserializa GenericRecord en Avro binary sin Schema Registry (uso interno). */
public final class AvroEventSerde {

    private AvroEventSerde() {}

    public static byte[] serialize(GenericRecord record) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            new GenericDatumWriter<>(record.getSchema()).write(record, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new EventSerializationException("Failed to serialize Avro record", e);
        }
    }

    public static GenericRecord deserialize(byte[] bytes, Schema schema) {
        try {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
            return new GenericDatumReader<GenericRecord>(schema).read(null, decoder);
        } catch (IOException e) {
            throw new EventSerializationException("Failed to deserialize Avro record", e);
        }
    }

    public static final class EventSerializationException extends RuntimeException {
        public EventSerializationException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
