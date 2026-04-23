package gt.agromis.shared.tenancy;

import gt.agromis.shared.domain.BusinessException;

/** Se lanza cuando el JWT no contiene country_code o el tenant no esta activo. */
public class UnauthorizedTenantException extends BusinessException {

    public UnauthorizedTenantException(String detail) {
        super("UNAUTHORIZED_TENANT", detail);
    }
}
