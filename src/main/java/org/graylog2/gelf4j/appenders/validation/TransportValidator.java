package org.graylog2.gelf4j.appenders.validation;

import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * This is validator for transport {@link ValidTransport} to have correctly set.
 *
 * @author Andrey Minov
 */
class TransportValidator implements ConstraintValidator<ValidTransport> {
  private static final Logger LOGGER = StatusLogger.getLogger();

  private static final Pattern TRANSPORTS_SUPPORTED = Pattern.compile("(tcp|udp)");

  private ValidTransport annotation;

  @Override
  public void initialize(final ValidTransport anAnnotation) {
    this.annotation = anAnnotation;
  }

  @Override
  public boolean isValid(final String name, final Object value) {
    return TRANSPORTS_SUPPORTED.matcher((String)value).matches() || err(name);
  }

  private boolean err(final String name) {
    LOGGER.error("Incorrectly provided transport: " + name);
    return false;
  }
}
