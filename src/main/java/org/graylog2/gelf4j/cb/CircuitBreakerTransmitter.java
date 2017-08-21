package org.graylog2.gelf4j.cb;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.graylog2.gelf4j.message.Payload;
import org.graylog2.gelf4j.transmitters.PayloadTransmitter;

/**
 * CircuitBreaker.
 * <p/>
 * Implementation of <a href="https://martinfowler.com/bliki/CircuitBreaker.html">Circuit breaker</a> using to
 * control failover withing failed third party resources.
 *
 * @author Andrey Minov.
 * @since 2017.02
 */
public class CircuitBreakerTransmitter implements PayloadTransmitter {

  private final long maxFailures;
  private final long recoverPeriod;
  private final TimeUnit timeUnit;

  private final AtomicLong failCounter;
  private final AtomicReference<State> state;
  private volatile long stopTime;

  private PayloadTransmitter delegate;


  /**
   * Instantiates a new Circuit breaker.
   *
   * @param maxFailures   the max failures number before circuit goes to close state
   * @param recoverPeriod the recover period aften which circuit breaker will try once again to ask for
   *                      resource
   * @param timeUnit      the time unit of recovery period
   * @param delegate      actual transmitter for the payload
   */
  public CircuitBreakerTransmitter(long maxFailures, long recoverPeriod, TimeUnit timeUnit,
                                   PayloadTransmitter delegate) {
    this.maxFailures = maxFailures;
    this.recoverPeriod = recoverPeriod;
    this.timeUnit = timeUnit;
    this.state = new AtomicReference<>(State.CLOSED);
    this.failCounter = new AtomicLong();
    this.delegate = delegate;
  }

  /**
   * Send value to delegate if possible or throw exception in case problem happens.
   *
   * @throws CircuitOpenException in case when circuit is open
   */
  @Override
  public void transmit(Payload payload) throws Exception {
    State initialState = getInitialState();
    if (EnumSet.of(State.CLOSED, State.PARTIALLY_OPEN).contains(initialState)) {
      try {
        delegate.transmit(payload);
        markClosed();
        return;
      } catch (Exception e) {
        if (connectionException(e)) {
          processException();
        }
      }
    }
    throw new CircuitOpenException("Circuit is open and request cannot be processed");
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public void open() throws IOException {
    delegate.open();
  }

  private State getInitialState() {
    // put it here as function must be side-affect-free (values must not be changed inside function).
    // be aware that it will bring one more volative get operation overhead.
    long stopShapshot = stopTime;
    State newState = state.getAndUpdate(val -> {
      if (val != State.OPEN
          || System.currentTimeMillis() - timeUnit.toMillis(recoverPeriod) < stopShapshot) {
        return val;
      }
      return State.PARTIALLY_OPEN;
    });
    if (newState == State.PARTIALLY_OPEN) {
      failCounter.decrementAndGet();
    }
    return newState;
  }

  private void markClosed() {
    State prev = state.getAndSet(State.CLOSED);
    if (prev != State.CLOSED) {
      failCounter.set(0);
    }
  }

  private void processException() {
    long value = failCounter.updateAndGet(v -> Math.min(v + 1, maxFailures));
    if (value >= maxFailures) {
      if (state.getAndSet(State.OPEN) != State.OPEN) {
        stopTime = System.currentTimeMillis();
      }
    }
  }

  private boolean connectionException(Throwable exception) {
    return exception instanceof SocketException || exception instanceof URISyntaxException
           || exception instanceof UnknownHostException
           || exception instanceof SocketTimeoutException || exception instanceof IOException;
  }


  private enum State {
    /**
     * Open state.
     */
    OPEN, /**
     * Partially open state.
     */
    PARTIALLY_OPEN, /**
     * Closed state.
     */
    CLOSED
  }
}
