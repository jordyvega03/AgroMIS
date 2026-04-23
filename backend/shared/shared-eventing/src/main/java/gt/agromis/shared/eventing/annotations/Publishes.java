package gt.agromis.shared.eventing.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Documenta que un handler publica un evento al topic indicado. */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Publishes {

    /** Nombre del topic Kafka destino. */
    String topic();

    /** Tipo del evento publicado. */
    Class<?> eventType();
}
