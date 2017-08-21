package org.graylog2.gelf4j.cb;

/**
 * CircuitCloseException.
 * <p/>
 * Exception raised when current circuit is open and operation requested cannot be performed
 *
 * @author Andrey Minov
 * @since 2017.02
 */
public class CircuitOpenException
    extends RuntimeException {

    public CircuitOpenException(String message) {
        super(message);
    }
}
