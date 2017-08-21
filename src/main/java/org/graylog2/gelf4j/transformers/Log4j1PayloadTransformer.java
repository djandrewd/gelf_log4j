package org.graylog2.gelf4j.transformers;

import static org.graylog2.gelf4j.Constants.CLASS_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.DEFAULT_VERSION;
import static org.graylog2.gelf4j.Constants.FACILITY_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.FILE_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.LINE_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.LOGGER_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.LOGGER_NDC_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.METHOD_ATTRIBUTE;
import static org.graylog2.gelf4j.Constants.THREAD_ATTRIBUTE;
import static org.graylog2.gelf4j.utils.Validations.isEmpty;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.graylog2.gelf4j.appenders.LoggingConfiguration;
import org.graylog2.gelf4j.message.Payload;

/**
 * This is transformer from log4j events version 1 into {@link org.graylog2.gelf4j.message.Payload}
 * GELF to use it later in transmitters.
 *
 * @author Andrey Minov
 */
public class Log4j1PayloadTransformer {
  private static final ThreadLocal<Payload> PAYLOAD_TL = new ThreadLocal<>();
  private LoggingConfiguration loggingConfiguration;

  public Log4j1PayloadTransformer(LoggingConfiguration loggingConfiguration) {
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

  /**
   * Make message payload from Log4j event will all required fields.
   *
   * @param layout the layout of the formatter event.
   * @param event  the event from log4j appender system
   * @return the GELF payload message with additional fields.
   */
  public Payload makeMessage(Layout layout, LoggingEvent event) {
    Payload payload = getPayload();
    payload.setVersion(DEFAULT_VERSION);
    // Timestamp must be transformed into UNIX time. Do not use TimeUnit as it shrink milliseconds.
    payload.setTimestamp(event.getTimeStamp() / 1000d);

    // Insert syslog level of the log4j message.
    Level level = event.getLevel();
    payload.setLevel(level.getSyslogEquivalent());

    // Check file and line on which call was made.
    // Also can add here methods.
    if (event.locationInformationExists()) {
      LocationInfo locationInformation = event.getLocationInformation();
      if (loggingConfiguration.isLogLine()) {
        payload.addAdditionalField(LINE_ATTRIBUTE, locationInformation.getLineNumber());
      }
      if (loggingConfiguration.isLogFile()) {
        payload.addAdditionalField(FILE_ATTRIBUTE, locationInformation.getFileName());
      }
      if (loggingConfiguration.isLogMethod()) {
        payload.addAdditionalField(METHOD_ATTRIBUTE, locationInformation.getMethodName());
      }
      if (loggingConfiguration.isLogClass()) {
        payload.addAdditionalField(CLASS_ATTRIBUTE, locationInformation.getClassName());
      }
    }

    String message = event.getRenderedMessage();
    if (message != null) {
      message = layout != null ? layout.format(event) : String.valueOf(event.getMessage());
      // Set short message of the application.
      payload.setShortMessage(message);
    }

    // Set full message in case we have some exception.
    ThrowableInformation ti = event.getThrowableInformation();
    if (ti != null && ti.getThrowable() != null && loggingConfiguration.isLogExceptions()) {
      StringWriter sw = new StringWriter();
      ti.getThrowable().printStackTrace(new PrintWriter(sw));
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
      @SuppressWarnings("unchecked") Map<String, Object> mdc =
          (Map<String, Object>) event.getProperties();
      if (mdc != null) {
        for (Map.Entry<String, Object> entry : mdc.entrySet()) {
          payload.addAdditionalField(entry.getKey(), String.valueOf(entry.getValue()));
        }
      }
    }
    // Get NDC and add a GELF field
    if (loggingConfiguration.isLogNdc() && event.getNDC() != null) {
      payload.addAdditionalField(LOGGER_NDC_ATTRIBUTE, event.getNDC());
    }

    // Log external fields
    if (loggingConfiguration.getExternalFields() != null) {
      loggingConfiguration.getExternalFields().forEach(payload::addAdditionalField);
    }

    return payload;
  }
}
