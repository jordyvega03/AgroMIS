package gt.agromis.shared.tenancy;

import gt.agromis.shared.domain.BusinessException;

public class InvalidCountryCodeException extends BusinessException {

    public InvalidCountryCodeException(String message) {
        super("INVALID_COUNTRY_CODE", message);
    }
}
