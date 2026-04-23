package gt.agromis.shared.tenancy;

import io.quarkus.arc.Unremovable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Interceptor que ejecuta SET LOCAL app.current_country antes de cada transaccion.
 * Esto activa las politicas RLS definidas en PostgreSQL.
 */
@Transactional
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
@ApplicationScoped
@Unremovable
public class TenantJpaInterceptor {

    private static final Logger LOG = Logger.getLogger(TenantJpaInterceptor.class);

    @Inject
    EntityManager em;

    @Inject
    TenantContext tenantContext;

    @AroundInvoke
    public Object setTenantOnTransaction(InvocationContext ctx) throws Exception {
        if (tenantContext.isSet()) {
            String code = tenantContext.get().value();
            em.createNativeQuery("SET LOCAL app.current_country = '" + code + "'")
                    .executeUpdate();
            LOG.debugf("RLS tenant set: %s", code);
        }
        return ctx.proceed();
    }
}
