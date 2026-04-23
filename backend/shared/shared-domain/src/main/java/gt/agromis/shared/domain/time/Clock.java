package gt.agromis.shared.domain.time;

import java.time.Instant;

/**
 * Abstraccion inyectable sobre el reloj del sistema.
 * Permite controlar el tiempo en tests sin depender de Mockito.
 */
public interface Clock {

    Instant now();

    static Clock system() {
        return Instant::now;
    }

    static Clock fixed(Instant instant) {
        return () -> instant;
    }
}
