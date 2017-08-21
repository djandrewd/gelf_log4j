package org.graylog2.gelf4j.appenders.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.logging.log4j.core.config.plugins.validation.Constraint;

/**
 * Annotation to validate transport connection type: TCP, UDP, HTTP and soon.
 *
 * @author Andrey Minov
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(TransportValidator.class)
public @interface ValidTransport {
}
