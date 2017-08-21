package org.graylog2.gelf4j.transformers;

import static org.graylog2.gelf4j.Constants.CLASS_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.DEFAULT_VERSION;
import static org.graylog2.gelf4j.Constants.FACILITY_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.FILE_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.LINE_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.LOGGER_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.METHOD_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.THREAD_ATTRIBUTE;
import static org.graylog2.gelf4j.utils.Validations.isEmpty;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.graylog2.gelf4j.appenders.LoggingConfiguration;
import org.graylog2.gelf4j.message.Payload;

/**
 * This is transformer from log4j events version 2 into {@link org.graylog2.gelf4j.message.Payload}
 * GELF to use it later in transmitters.
 *
 * @author Andrey Minov
 */
public class Log4j2PayloadTransformer {
  private static final ThreadLocal<Payload> PAYLOAD_TL = new ThreadLocal<>();
  private static final ThreadLocal<StringBuilder> SB_TL = new ThreadLocal<>();
  private LoggingConfiguration loggingConfiguration;

  public Log4j2PayloadTransformer(LoggingConfiguration loggingConfiguration) {
    this.loggingConfiguration = loggingConfiguration;
  }

  private static Payload getPayload() {
    Payload payload = PAYLOAD_TL.get();
    if (payload == null) {
      payload = new Payload();
      PAYLOAD_TL.set(payload);
    }
    payload.clear();
    return payload;
  }

  private static StringBuilder getMessageStringBuilder() {
    StringBuilder builder = SB_TL.get();
    if (builder == null) {
      builder = new StringBuilder();
      SB_TL.set(builder);
    }
    builder.setLength(0);
    return builder;
  }

  /**
   * Make message payload from Log4j event will all required fields.
   *
   * @param layout                the layout of the formatter event.
   * @param event                 the event from log4j appender system
   * @param gcFree                true in case we have to reuse thread local string builder to avoid GC
   * @return the GELF payload message with additional fields.
   */
  public Payload makeMessage(Layout<? extends LogEvent> layout, LogEvent event, boolean gcFree) {
    Payload payload = getPayload();
    payload.setVersion(DEFAULT_VERSION);
    // Timestamp must be transformed into UNIX time. Do not use TimeUnit as it shrink milliseconds.
    payload.setTimestamp(event.getTimeMillis() / 1000d);

    // Insert syslog level of the log4j message.
    Level level = event.getLevel();
    payload.setLevel(Severity.getSeverity(level).getCode());

    // Check file and line on which call was made.
    // Also can add here methods.
    if (event.isIncludeLocation()) {
      StackTraceElement source = event.getSource();
      if (loggingConfiguration.isLogLine()) {
        payload.addAdditionalField(LINE_ATTRIBUTE, Integer.toString(source.getLineNumber()));
      }
      if (loggingConfiguration.isLogFile()) {
        payload.addAdditionalField(FILE_ATTRIBUTE, source.getFileName());
      }
      if (loggingConfiguration.isLogMethod()) {
        payload.addAdditionalField(METHOD_ATTRIBUTE, source.getMethodName());
      }
      if (loggingConfiguration.isLogClass()) {
        payload.addAdditionalField(CLASS_ATTRIBUTE, source.getClassName());
      }
    }
    // In case layout is set we format message according to it, otherwise just print message.
    if (layout != null) {
      payload.setShortMessageBuffer(layout.toByteArray(event));
    } else {
      final Message message = event.getMessage();
      if (message instanceof CharSequence) {
        payload.setShortMessage((CharSequence) message);
      } else if (gcFree && message instanceof StringBuilderFormattable) {
        StringBuilder messageBuffer = getMessageStringBuilder();
        ((StringBuilderFormattable) message).formatTo(messageBuffer);
        payload.setShortMessage(messageBuffer);
      }
    }
    // Set full message in case we have some exception.
    if (event.getThrown() != null && loggingConfiguration.isLogExceptions()) {
      StringWriter sw = new StringWriter();
      event.getThrown().printStackTrace(new PrintWriter(sw));
      payload.setFullMessage(sw.toString());
    }

    // Set logging facility in case it is existed.
    if (!isEmpty(loggingConfiguration.getFacility())) {
      payload.addAdditionalField(FACILITY_ATTRIBUTE, loggingConfiguration.getFacility());
    }

    try {
      payload.setHost(InetAddress.getLocalHost().getHostAddress());
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }

    if (loggingConfiguration.isLogThread()) {
      payload.addAdditionalField(THREAD_ATTRIBUTE, event.getThreadName());
    }
    if (loggingConfiguration.isLogLogger()) {
      payload.addAdditionalField(LOGGER_ATTRIBUTE, event.getLoggerName());
    }

    if (loggingConfiguration.isLogMdcValues()) {
      // Get MDC and add a GELF field for each key/value pair
      ReadOnlyStringMap mdc = event.getContextData();
      if (mdc != null) {
        mdc.forEach(payload::addAdditionalField);
      }
    }
    // Log external fields
    if (loggingConfiguration.getExternalFields() != null) {
      loggingConfiguration.getExternalFields().forEach(payload::addAdditionalField);
    }

    return payload;
  }

}
