package gt.agromis.shared.tenancy;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;

/** Extrae country_code del JWT o del header X-Country-Code (solo en dev/test). */
@ApplicationScoped
public class TenantResolver {

    static final String CLAIM_COUNTRY_CODE = "country_code";
    static final String HEADER_COUNTRY_CODE = "X-Country-Code";

    @Inject
    SecurityIdentity identity;

    public CountryCode resolve(jakarta.ws.rs.core.HttpHeaders headers) {
        // 1. JWT claim tiene precedencia
        Object claim = identity.getAttribute(CLAIM_COUNTRY_CODE);
        if (claim != null) {
            return CountryCode.of(claim.toString());
        }

        // 2. Header de fallback (util en tests y en dev sin Keycloak)
        Optional<String> headerVal =
                Optional.ofNullable(headers.getHeaderString(HEADER_COUNTRY_CODE));
        if (headerVal.isPresent()) {
            return CountryCode.of(headerVal.get());
        }

        throw new UnauthorizedTenantException(
                "JWT missing 'country_code' claim and no '" + HEADER_COUNTRY_CODE + "' header");
    }
}
