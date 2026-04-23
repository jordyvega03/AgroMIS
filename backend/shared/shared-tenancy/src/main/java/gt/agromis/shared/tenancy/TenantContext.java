package gt.agromis.shared.tenancy;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.RequestScoped;

/** Holder del pais activo para el request en curso. */
@RequestScoped
@Unremovable
public class TenantContext {

    private CountryCode countryCode;

    public void set(CountryCode countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * @throws UnauthorizedTenantException si no se establecio country_code para este request
     */
    public CountryCode get() {
        if (countryCode == null) {
            throw new UnauthorizedTenantException("No country_code established for this request");
        }
        return countryCode;
    }

    public boolean isSet() {
        return countryCode != null;
    }
}
