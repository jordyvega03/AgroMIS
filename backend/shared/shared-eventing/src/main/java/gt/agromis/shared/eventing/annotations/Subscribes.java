package gt.agromis.shared.eventing.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Documenta que un bean consume mensajes del topic indicado. */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribes {

    /** Nombre del topic Kafka origen. */
    String topic();

    /** Consumer group al que pertenece. */
    String consumerGroup();
}
