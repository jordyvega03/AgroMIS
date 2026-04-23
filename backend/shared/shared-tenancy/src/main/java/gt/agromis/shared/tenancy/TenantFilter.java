package gt.agromis.shared.tenancy;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

/** JAX-RS filter que pobla TenantContext antes de que llegue al resource. */
@Provider
@Priority(Priorities.AUTHENTICATION + 10)
public class TenantFilter implements ContainerRequestFilter {

    @Inject
    TenantResolver resolver;

    @Inject
    TenantContext tenantContext;

    @Inject
    HttpHeaders httpHeaders;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Los endpoints de health y metrics no requieren tenant
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("/q/") || path.startsWith("/metrics")) {
            return;
        }

        CountryCode code = resolver.resolve(httpHeaders);
        tenantContext.set(code);
    }
}
