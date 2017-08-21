package org.graylog2.gelf4j;

/**
 * Constants used all over for GELF protocol.
 *
 * @author Andrey Minov
 */
public final class Constants {

  /**
   * The constant for GELF message version. Default is 1.1. Mandatory.
   */
  public static final String VERSION = "version";
  /**
   * The constant for GELF source host. Mandatory
   */
  public static final String HOST = "host";
  /**
   * The constant for GELF short message. Mandatory
   */
  public static final String SHORT_MESSAGE = "short_message";
  /**
   * The constant for GELF full message. Used for exception stacktraces. Optional
   */
  public static final String FULL_MESSAGE = "full_message";
  /**
   * The constant for GELF message timestamp in UNIX time (seconds). Optional
   */
  public static final String TIMESTAMP = "timestamp";
  /**
   * The constant for GELF syslog level. Optional
   */
  public static final String LEVEL = "level";
  /**
   * The constant The constant for GELF facility which sent message. Optional. Deprecated
   */
  public static final String FACILITY = "facility";
  /**
   * The constant The constant for GELF line of the code. Optional. Deprecated
   */
  public static final String LINE = "line";
  /**
   * The constant The constant for GELF source file where message is recorded. Optional. Deprecated
   */
  public static final String FILE = "file";

  /**
   * Default GELF protocol version.
   */
  public static final String DEFAULT_VERSION = "1.1";

  /**
   * The constant for default socket timeout.
   */
  public static final int DEFAULT_SO_TIMEOUT = 2000;
  /**
   * The constant for default TCP port.
   */
  public static final int DEFAULT_TCP_PORT = 12201;

  /**
   * The constant for default loggin facility.
   */
  public static final String DEFAULT_FACILITY = "gelf4j-appender";

  /**
   * The constant for line in the code writing message.
   */
  public static final String LINE_ATTRIBUTE = "line";
  /**
   * The constant for in which message was written.
   */
  public static final String FILE_ATTRIBUTE = "file";
  /**
   * The constant for method in which log was written.
   */
  public static final String METHOD_ATTRIBUTE = "method";
  /**
   * The constant for class in which log was written.
   */
  public static final String CLASS_ATTRIBUTE = "class";
  /**
   * The constant for thread in which log was written.
   */
  public static final String THREAD_ATTRIBUTE = "thread";
  /**
   * The constant for current logger name in which log was written.
   */
  public static final String LOGGER_ATTRIBUTE = "logger";
  /**
   * The constant for NDC for logging event.
   */
  public static final String LOGGER_NDC_ATTRIBUTE = "loggerNdc";
  /**
   * The constant for logger facility which wrote the message.
   */
  public static final String FACILITY_ATTRIBUTE = "facility";

  /**
   * The constant for max buffered size of the payload.
   */
  public static final int MESSAGE_BUFFER_SIZE = 8192;

  /**
   * The constant for max dafault GELF max allowed datagram.
   */
  public static final int MAX_UDP_DATAGRAM_SIZE = 8192;

  /**
   * The constant stands for default compression rate in the system.
   */
  public static final int DEFAULT_COMPRESSION_RATE = 5;

  /**
   * The constant for defult number of failures when circuit breaker will pass to open state.
   */
  public static final int DEFAULT_FAILURES_TO_OPEN = 10;
  /**
   * The constant for default number of seconds need for circuit breaker to recover.
   */
  public static final long DEFAULT_RECOVERY_PERIOD_SEC = 20;

}
