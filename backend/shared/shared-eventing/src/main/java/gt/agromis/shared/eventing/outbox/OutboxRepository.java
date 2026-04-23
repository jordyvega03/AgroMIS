package gt.agromis.shared.eventing.outbox;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OutboxRepository implements PanacheRepositoryBase<OutboxEntry, UUID> {

    private static final int BATCH_SIZE = 100;

    /** Retorna hasta 100 entradas no publicadas, ordenadas por sequence_number. */
    public List<OutboxEntry> findUnpublished() {
        return find(
                        "publishedAt IS NULL ORDER BY sequence_number ASC")
                .page(0, BATCH_SIZE)
                .list();
    }

    /** Cuenta entradas pendientes — usado para la metrica outbox_unpublished_count. */
    public long countUnpublished() {
        return count("publishedAt IS NULL");
    }
}
