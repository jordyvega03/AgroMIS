package gt.agromis.shared.eventing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

/** Propaga el contexto de tracing de OpenTelemetry en headers Kafka. */
@ApplicationScoped
public class TraceContextPropagator {

    private static final TextMapSetter<Headers> SETTER =
            (headers, key, value) ->
                    headers.add(key, value.getBytes(StandardCharsets.UTF_8));

    private static final TextMapGetter<Headers> GETTER =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(Headers carrier) {
                    return Arrays.stream(carrier.toArray())
                            .map(Header::key)
                            .toList();
                }

                @Override
                public String get(Headers carrier, String key) {
                    Header header = carrier.lastHeader(key);
                    if (header == null) return null;
                    return new String(header.value(), StandardCharsets.UTF_8);
                }
            };

    public void inject(Headers headers) {
        GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), headers, SETTER);
    }

    public Context extract(Headers headers) {
        return GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), headers, GETTER);
    }
}
