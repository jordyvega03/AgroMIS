package gt.agromis.shared.tenancy;

import gt.agromis.shared.domain.ValueObject;
import java.util.Objects;
import java.util.Set;

/** Value object que encapsula un codigo ISO 3166-1 alpha-2 valido para AgroMIS. */
public final class CountryCode implements ValueObject {

    private static final Set<String> SUPPORTED =
            Set.of("GT", "SV", "HN", "NI", "CR", "PA", "BZ");

    private final String value;

    private CountryCode(String value) {
        this.value = value;
    }

    /**
     * @throws InvalidCountryCodeException si el codigo no es soportado
     */
    public static CountryCode of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidCountryCodeException("country_code must not be blank");
        }
        String upper = raw.toUpperCase();
        if (!SUPPORTED.contains(upper)) {
            throw new InvalidCountryCodeException("country_code '" + raw + "' is not supported");
        }
        return new CountryCode(upper);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CountryCode that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
