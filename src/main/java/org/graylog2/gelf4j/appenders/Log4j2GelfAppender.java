package org.graylog2.gelf4j.appenders;


import static org.apache.logging.log4j.core.util.Constants.ENABLE_DIRECT_ENCODERS;
import static org.graylog2.gelf4j.Constants.DEFAULT_COMPRESSION_RATE;
import static org.graylog2.gelf4j.Constants.DEFAULT_FAILURES_TO_OPEN;
import static org.graylog2.gelf4j.Constants.DEFAULT_RECOVERY_PERIOD_SEC;
import static org.graylog2.gelf4j.Constants.MAX_UDP_DATAGRAM_SIZE;
import static org.graylog2.gelf4j.utils.Validations.checkValid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidPort;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.status.StatusLogger;
import org.graylog2.gelf4j.Constants;
import org.graylog2.gelf4j.appenders.validation.ValidTransport;
import org.graylog2.gelf4j.cb.CircuitBreakerTransmitter;
import org.graylog2.gelf4j.message.Payload;
import org.graylog2.gelf4j.message.PayloadEncoder;
import org.graylog2.gelf4j.message.StringBuilderEncoder;
import org.graylog2.gelf4j.transformers.Log4j2PayloadTransformer;
import org.graylog2.gelf4j.transmitters.PayloadTransmitter;
import org.graylog2.gelf4j.transmitters.TcpTransmitter;
import org.graylog2.gelf4j.transmitters.UdpTransmitter;

/**
 * Appender for Log4j version 2 which construct {@link org.graylog2.gelf4j.message.Payload}
 * and send it to Greylog2 server using specified type
 * of message transmitter.
 * <p/>
 * <b>NOTE! Experemental.</b>
 *
 * @author Andrey Minov
 */
@Plugin(name = "Gelf4j2", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class Log4j2GelfAppender extends AbstractAppender {

  private static final Logger LOGGER = StatusLogger.getLogger();

  static {
    JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
    JsonStream.registerNativeEncoder(Payload.class, new PayloadEncoder());
    JsonStream.registerNativeEncoder(StringBuilder.class, new StringBuilderEncoder());
  }

  private String type;
  private String hostname;
  private int soTimeout;
  private int port;
  private boolean blocking;
  private int sendBufferSize;
  private boolean useCompression;
  private int compressionLevel;
  private int compressionLimit;
  private boolean useCircuitBreaker;
  private int failuresToOpen;
  private long secondsToRecover;

  private Log4j2PayloadTransformer payloadTransformer;
  private PayloadTransmitter payloadTransmitter;
  private LoggingConfiguration configuration;

  private Log4j2GelfAppender(String name, Filter filter, Layout<? extends LogEvent> layout,
                             String type, String hostname, int soTimeout, int port,
                             boolean blocking, int sendBufferSize, boolean useCompression,
                             int compressionLevel, int compressionLimit, boolean useCircuitBreaker,
                             int failuresToOpen, long secondsToRecover,
                             LoggingConfiguration configuration) {
    super(name, filter, layout, !configuration.isLogExceptions());
    this.type = type;
    this.hostname = hostname;
    this.soTimeout = soTimeout;
    this.port = port;
    this.blocking = blocking;
    this.sendBufferSize = sendBufferSize;
    this.useCompression = useCompression;
    this.compressionLevel = compressionLevel;
    this.compressionLimit = compressionLimit;
    this.useCircuitBreaker = useCircuitBreaker;
    this.failuresToOpen = failuresToOpen;
    this.secondsToRecover = secondsToRecover;
    this.configuration = configuration;
  }

  @PluginBuilderFactory
  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public void start() {
    super.start();
    payloadTransformer = new Log4j2PayloadTransformer(configuration);
    payloadTransmitter = createTransmitter();
    if (payloadTransmitter != null) {
      try {
        payloadTransmitter.open();
      } catch (Exception e) {
        LOGGER.error("Cannot open connection to Graylog server.", e);
      }
    }
  }

  @Override
  public void stop() {
    super.stop();
    if (payloadTransmitter != null) {
      try {
        payloadTransmitter.close();
      } catch (Exception e) {
        LOGGER.error("Cannot close connection to Graylog server.", e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void append(LogEvent event) {
    try {
      if (payloadTransmitter == null) {
        LOGGER.error("Transmitter is not open, probably incorrectly configured!");
        return;
      }
      Payload payload = payloadTransformer
          .makeMessage((Layout<? extends LogEvent>) getLayout(), event, ENABLE_DIRECT_ENCODERS);
      if (!checkValid(payload)) {
        LOGGER.error("Missing required parameters: {}", event);
        return;
      }
      payloadTransmitter.transmit(payload);
    } catch (Exception e) {
      LOGGER.error(() -> "Cannot write to Gelf transmitter: " + e.getMessage(), e);
    }
  }

  private PayloadTransmitter createTransmitter() {
    PayloadTransmitter delegate = null;
    if ("tcp".equals(type)) {
      delegate = new TcpTransmitter(hostname, port, soTimeout, blocking, sendBufferSize);
    }
    if ("udp".equals(type)) {
      delegate =
          new UdpTransmitter(hostname, port, sendBufferSize, useCompression, compressionLevel,
              compressionLimit);
    }
    if (delegate == null) {
      return null;
    }
    if (useCircuitBreaker) {
      return new CircuitBreakerTransmitter(failuresToOpen, secondsToRecover, TimeUnit.SECONDS,
          delegate);
    }
    return delegate;
  }

  public static class Builder implements org.apache.logging.log4j.core.util.Builder<Log4j2GelfAppender> {

    @PluginBuilderAttribute
    @Required(message = "No name provided for Log4j2GelfAppender")
    private String name;

    @PluginBuilderAttribute
    @Required(message = "Transport type be provided")
    @ValidTransport
    private String type;

    @PluginBuilderAttribute
    @ValidHost
    @Required(message = "Hostname must be provided")
    private String hostname;

    @PluginBuilderAttribute
    @ValidPort
    private int port = Constants.DEFAULT_TCP_PORT;

    @PluginBuilderAttribute
    private int soTimeout = Constants.DEFAULT_SO_TIMEOUT;

    @PluginBuilderAttribute
    private String facility;

    @PluginBuilderAttribute
    private boolean logExceptions = true;

    @PluginBuilderAttribute
    private boolean logThread = true;

    @PluginBuilderAttribute
    private boolean logLine;

    @PluginBuilderAttribute
    private boolean logFile;

    @PluginBuilderAttribute
    private boolean logMethod;

    @PluginBuilderAttribute
    private boolean logClass;

    @PluginBuilderAttribute
    private boolean logLogger;

    @PluginBuilderAttribute
    private boolean logMdcValues = true;

    @PluginBuilderAttribute
    private boolean blocking;

    @PluginBuilderAttribute
    private int sendBufferSize = -1;

    @PluginBuilderAttribute
    private boolean useCompression;

    @PluginBuilderAttribute
    private int compressionLevel = DEFAULT_COMPRESSION_RATE;

    @PluginBuilderAttribute
    private int compressionLimit = MAX_UDP_DATAGRAM_SIZE;

    @PluginBuilderAttribute
    private boolean useCircuitBreaker;

    @PluginBuilderAttribute
    private int failuresToOpen = DEFAULT_FAILURES_TO_OPEN;

    @PluginBuilderAttribute
    private long secondsToRecover = DEFAULT_RECOVERY_PERIOD_SEC;

    @PluginElement("AdditionalField")
    private KeyValuePair[] additionalFields;

    @PluginElement("Layout")
    private Layout<? extends LogEvent> layout;

    @PluginElement("Filter")
    private Filter filter;


    public Builder setName(final String name) {
      this.name = name;
      return this;
    }

    public Builder setLayout(final Layout<? extends LogEvent> layout) {
      this.layout = layout;
      return this;
    }

    public Builder setFilter(final Filter filter) {
      this.filter = filter;
      return this;
    }

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setHostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    public Builder setSoTimeout(int soTimeout) {
      this.soTimeout = soTimeout;
      return this;
    }

    public Builder setFacility(String facility) {
      this.facility = facility;
      return this;
    }

    public Builder setLogExceptions(boolean logExceptions) {
      this.logExceptions = logExceptions;
      return this;
    }

    public Builder setLogThread(boolean logThread) {
      this.logThread = logThread;
      return this;
    }

    public Builder setLogLine(boolean logLine) {
      this.logLine = logLine;
      return this;
    }

    public Builder setLogFile(boolean logFile) {
      this.logFile = logFile;
      return this;
    }

    public Builder setLogMethod(boolean logMethod) {
      this.logMethod = logMethod;
      return this;
    }

    public Builder setLogClass(boolean logClass) {
      this.logClass = logClass;
      return this;
    }

    public Builder setLogLogger(boolean logLogger) {
      this.logLogger = logLogger;
      return this;
    }

    public Builder setLogMdcValues(boolean logMdcValues) {
      this.logMdcValues = logMdcValues;
      return this;
    }

    public Builder setSendBufferSize(int sendBufferSize) {
      this.sendBufferSize = sendBufferSize;
      return this;
    }

    public Builder setBlocking(boolean blocking) {
      this.blocking = blocking;
      return this;
    }

    public Builder setUseCompression(boolean useCompression) {
      this.useCompression = useCompression;
      return this;
    }

    public Builder setCompressionLevel(int compressionLevel) {
      this.compressionLevel = compressionLevel;
      return this;
    }

    public Builder setCompressionLimit(int compressionLimit) {
      this.compressionLimit = compressionLimit;
      return this;
    }

    public Builder setUseCircuitBreaker(boolean useCircuitBreaker) {
      this.useCircuitBreaker = useCircuitBreaker;
      return this;
    }

    public Builder setFailuresToOpen(int failuresToOpen) {
      this.failuresToOpen = failuresToOpen;
      return this;
    }

    public Builder setSecondsToRecover(long secondsToRecover) {
      this.secondsToRecover = secondsToRecover;
      return this;
    }

    @Override
    public Log4j2GelfAppender build() {
      HashMap<String, String> properties = new HashMap<>();
      if (additionalFields != null) {
        Arrays.stream(additionalFields).forEach(v -> properties.put(v.getKey(), v.getValue()));
      }
      LoggingConfiguration configuration =
          new LoggingConfiguration(facility, logExceptions, logThread, logLine, logFile, logMethod,
              logClass, logLogger, logMdcValues, false, properties);
      return new Log4j2GelfAppender(name, filter, layout, type, hostname, soTimeout, port, blocking,
          sendBufferSize, useCompression, compressionLevel, compressionLimit, useCircuitBreaker,
          failuresToOpen, secondsToRecover, configuration);
    }
  }
}
