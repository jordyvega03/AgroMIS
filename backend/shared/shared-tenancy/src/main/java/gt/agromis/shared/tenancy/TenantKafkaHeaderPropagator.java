package gt.agromis.shared.tenancy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.Headers;

/** Inyecta el country_code en los headers de cada mensaje Kafka saliente. */
@ApplicationScoped
public class TenantKafkaHeaderPropagator {

    public static final String HEADER_COUNTRY_CODE = "X-Country-Code";

    @Inject
    TenantContext tenantContext;

    public void inject(Headers headers) {
        if (tenantContext.isSet()) {
            String code = tenantContext.get().value();
            headers.add(
                    HEADER_COUNTRY_CODE, code.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static CountryCode extract(Headers headers) {
        var header = headers.lastHeader(HEADER_COUNTRY_CODE);
        if (header == null) {
            throw new UnauthorizedTenantException(
                    "Kafka message missing header: " + HEADER_COUNTRY_CODE);
        }
        return CountryCode.of(new String(header.value(), StandardCharsets.UTF_8));
    }
}
