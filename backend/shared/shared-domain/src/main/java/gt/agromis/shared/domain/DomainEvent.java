package gt.agromis.shared.domain;

import java.time.Instant;

/**
 * Contrato que todo evento de dominio debe cumplir.
 * El countryCode garantiza multi-tenancy en cada evento.
 */
public interface DomainEvent {

    /** UUID v7 unico por evento. */
    String eventId();

    String aggregateId();

    /** Formato "NombreEvento.v1", p.ej. "PlantingReportSubmitted.v1". */
    String eventType();

    Instant occurredAt();

    /** Codigo ISO 3166-1 alpha-2 del pais, siempre presente. */
    String countryCode();
}
