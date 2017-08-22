package org.graylog2.gelf4j.appenders;

import static com.jsoniter.JsonIterator.deserialize;
import static org.graylog2.gelf4j.Constants.DEFAULT_COMPRESSION_RATE;
import static org.graylog2.gelf4j.Constants.DEFAULT_FACILITY;
import static org.graylog2.gelf4j.Constants.DEFAULT_FAILURES_TO_OPEN;
import static org.graylog2.gelf4j.Constants.DEFAULT_RECOVERY_PERIOD_SEC;
import static org.graylog2.gelf4j.Constants.DEFAULT_SO_TIMEOUT;
import static org.graylog2.gelf4j.Constants.DEFAULT_TCP_PORT;
import static org.graylog2.gelf4j.Constants.MAX_UDP_DATAGRAM_SIZE;
import static org.graylog2.gelf4j.utils.Validations.checkValid;
import static org.graylog2.gelf4j.utils.Validations.isEmpty;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.TypeLiteral;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.gelf4j.cb.CircuitBreakerTransmitter;
import org.graylog2.gelf4j.message.Payload;
import org.graylog2.gelf4j.message.PayloadEncoder;
import org.graylog2.gelf4j.message.StringBuilderEncoder;
import org.graylog2.gelf4j.transformers.Log4j1PayloadTransformer;
import org.graylog2.gelf4j.transmitters.PayloadTransmitter;
import org.graylog2.gelf4j.transmitters.TcpTransmitter;
import org.graylog2.gelf4j.transmitters.UdpTransmitter;

/**
 * Appender for Log4j version 1 which construct {@link org.graylog2.gelf4j.message.Payload} from
 * {@link org.apache.log4j.spi.LoggingEvent} and send it to Greylog2 server using specified type
 * of message transmitter.
 *
 * @author Andrey Minov
 */
public class Log4jGelfAppender extends AppenderSkeleton {

  static {
    JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
    JsonStream.registerNativeEncoder(Payload.class, new PayloadEncoder());
    JsonStream.registerNativeEncoder(StringBuilder.class, new StringBuilderEncoder());
  }

  private PayloadTransmitter payloadTransmitter;
  private Log4j1PayloadTransformer payloadTransformer;
  private boolean isOpen;
  // Log4j appender properties.
  private String graylogHost;
  private String soTimeout;
  private String port;
  private String facility;
  private boolean logExceptions;
  private boolean logThread;
  private boolean logLine;
  private boolean logFile;
  private boolean logMethod;
  private boolean logClass;
  private boolean logLogger;
  private boolean logMdcValues;
  private boolean logNdc;
  private boolean blocking;
  private int sendBufferSize;
  private boolean useCompression;
  private int compressionLevel;
  private int compressionLimit;
  private boolean useCircuitBreaker;
  private int failuresToOpen;
  private long secondsToRecover;

  private Map<String, String> additionalFields;

  public Log4jGelfAppender() {
    this.facility = DEFAULT_FACILITY;
    this.logExceptions = true;
    this.logThread = true;
    this.logMdcValues = true;
    this.sendBufferSize = -1;
    this.blocking = false;
    this.compressionLevel = DEFAULT_COMPRESSION_RATE;
    this.compressionLimit = MAX_UDP_DATAGRAM_SIZE;
    this.failuresToOpen = DEFAULT_FAILURES_TO_OPEN;
    this.secondsToRecover = DEFAULT_RECOVERY_PERIOD_SEC;
  }

  @Override
  public void activateOptions() {
    if (isEmpty(graylogHost)) {
      errorHandler.error("Missing required property field: graylogHost", null,
          ErrorCode.ADDRESS_PARSE_FAILURE);
      return;
    }

    int serverPort = isEmpty(port) ? DEFAULT_TCP_PORT : Integer.parseInt(port);
    int soTimeoutMs = isEmpty(soTimeout) ? DEFAULT_SO_TIMEOUT : Integer.parseInt(soTimeout);

    // Construct logging information and pass it to logger message converter
    LoggingConfiguration configuration =
        new LoggingConfiguration(facility, logExceptions, logThread, logLine, logFile, logMethod,
            logClass, logLogger, logMdcValues, logNdc, additionalFields);
    payloadTransformer = new Log4j1PayloadTransformer(configuration);
    try {
      payloadTransmitter = createTransmitter(serverPort, soTimeoutMs);
      payloadTransmitter.open();
      isOpen = true;
    } catch (Exception e) {
      errorHandler
          .error("Cannot open connection to Graylog server.", e, ErrorCode.FILE_OPEN_FAILURE);
    }
  }

  @Override
  protected void append(LoggingEvent event) {
    try {
      if (!isOpen) {
        errorHandler.error("Transmitter is not open, probably incorrectly configured!");
        return;
      }
      Payload payload = payloadTransformer.makeMessage(layout, event);
      if (!checkValid(payload)) {
        errorHandler.error("Missing required parameters!", null, ErrorCode.WRITE_FAILURE, event);
        return;
      }
      payloadTransmitter.transmit(payload);
    } catch (Exception e) {
      errorHandler.error("Cannot write to Gelf transmitter.", e, ErrorCode.WRITE_FAILURE, event);
    }
  }

  @Override
  public void close() {
    if (payloadTransmitter != null) {
      try {
        payloadTransmitter.close();
      } catch (IOException e) {
        errorHandler.error(e.getMessage(), e, ErrorCode.CLOSE_FAILURE);
      }
    }
  }

  @Override
  public boolean requiresLayout() {
    return true;
  }

  public void setGraylogHost(String graylogHost) {
    this.graylogHost = graylogHost;
  }

  public void setSoTimeout(String soTimeout) {
    this.soTimeout = soTimeout;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public void setFacility(String facility) {
    this.facility = facility;
  }

  public void setLogExceptions(boolean logExceptions) {
    this.logExceptions = logExceptions;
  }

  public void setLogThread(boolean logThread) {
    this.logThread = logThread;
  }

  public void setLogLine(boolean logLine) {
    this.logLine = logLine;
  }

  public void setLogFile(boolean logFile) {
    this.logFile = logFile;
  }

  public void setLogMethod(boolean logMethod) {
    this.logMethod = logMethod;
  }

  public void setLogClass(boolean logClass) {
    this.logClass = logClass;
  }

  public void setLogLogger(boolean logLogger) {
    this.logLogger = logLogger;
  }

  public void setLogMdcValues(boolean logMdcValues) {
    this.logMdcValues = logMdcValues;
  }

  public void setLogNdc(boolean logNdc) {
    this.logNdc = logNdc;
  }

  public void setAdditionalFields(String additionalFields) {
    try {
      this.additionalFields = deserialize(additionalFields, new TypeLiteral<Map<String, String>>() {
      });
    } catch (Exception e) {
      errorHandler.error(e.getMessage(), e, ErrorCode.GENERIC_FAILURE);
    }
    if (this.additionalFields == null) {
      this.additionalFields = deserialize(additionalFields.replaceAll("'", "\""),
          new TypeLiteral<Map<String, String>>() {
          });
    }
  }

  public void setBlocking(boolean blocking) {
    this.blocking = blocking;
  }

  public void setSendBufferSize(int sendBufferSize) {
    this.sendBufferSize = sendBufferSize;
  }

  public void setUseCompression(boolean useCompression) {
    this.useCompression = useCompression;
  }

  public void setCompressionLevel(int compressionLevel) {
    this.compressionLevel = compressionLevel;
  }

  public void setCompressionLimit(int compressionLimit) {
    this.compressionLimit = compressionLimit;
  }

  public void setUseCircuitBreaker(boolean useCircuitBreaker) {
    this.useCircuitBreaker = useCircuitBreaker;
  }

  public void setFailuresToOpen(int failuresToOpen) {
    this.failuresToOpen = failuresToOpen;
  }

  public void setSecondsToRecover(int secondsToRecover) {
    this.secondsToRecover = secondsToRecover;
  }

  private PayloadTransmitter createTransmitter(int serverPort, int soTimeoutMs) {
    PayloadTransmitter delegate = null;
    if (graylogHost.matches("tcp:.+")) {
      delegate = new TcpTransmitter(graylogHost.substring(4), serverPort, soTimeoutMs, blocking,
          sendBufferSize);
    }
    if (graylogHost.matches("udp:.+")) {
      delegate =
          new UdpTransmitter(graylogHost.substring(4), serverPort, sendBufferSize, useCompression,
              compressionLevel, compressionLimit);
    }
    if (delegate == null) {
      throw new IllegalStateException(
          "Graylog hostname has not supported protocol type:" + graylogHost);
    }
    if (useCircuitBreaker) {
      return new CircuitBreakerTransmitter(failuresToOpen, secondsToRecover, TimeUnit.SECONDS,
          delegate);
    }
    return delegate;
  }
}
