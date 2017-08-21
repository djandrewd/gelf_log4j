package org.graylog2.gelf4j.appenders;

import java.util.Map;

/**
 * Configuration properties for logging filtering and processing.
 * <p/>
 * This configuration is created from logger specific configuration (log4j apprender).
 *
 * @author Andrey Minov
 */
public class LoggingConfiguration {
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
  private Map<String, String> externalFields;

  /**
   * Instantiates a new Logging configuration.
   *
   * @param facility       the facility from which log event is came
   * @param logExceptions  true in case exceptions must be logged
   * @param logThread      true in case thread name must be logged.
   * @param logLine        true in case logging line should be logged.
   * @param logFile        true in case logging file should be logged.
   * @param logMethod      true in case logging method should be logged.
   * @param logClass       true in case logging class should be logged.
   * @param logLogger      true in case logger name should be logged.
   * @param logMdcValues   true in case MDC keys and values should be logged.
   * @param logNdc         true in case NDC should be logged.
   * @param externalFields the external fields used during logging.
   */
  LoggingConfiguration(String facility, boolean logExceptions, boolean logThread, boolean logLine,
                       boolean logFile, boolean logMethod, boolean logClass, boolean logLogger,
                       boolean logMdcValues, boolean logNdc, Map<String, String> externalFields) {
    this.facility = facility;
    this.logExceptions = logExceptions;
    this.logThread = logThread;
    this.logLine = logLine;
    this.logFile = logFile;
    this.logMethod = logMethod;
    this.logClass = logClass;
    this.logLogger = logLogger;
    this.logMdcValues = logMdcValues;
    this.logNdc = logNdc;
    this.externalFields = externalFields;
  }

  /**
   * Gets the facility from which log event is came.
   *
   * @return the facility from which log event is came.
   */
  public String getFacility() {
    return facility;
  }

  /**
   * True in case exceptions must be logged
   *
   * @return true in case exceptions must be logged
   */
  public boolean isLogExceptions() {
    return logExceptions;
  }

  /**
   * True in case thread name must be logged.
   *
   * @return true in case thread name must be logged.
   */
  public boolean isLogThread() {
    return logThread;
  }

  /**
   * True in case logging line should be logged.
   *
   * @return true in case logging line should be logged.
   */
  public boolean isLogLine() {
    return logLine;
  }

  /**
   * True in case logging file should be logged.
   *
   * @return true in case logging file should be logged.
   */
  public boolean isLogFile() {
    return logFile;
  }

  /**
   * True in case logging method should be logged.
   *
   * @return true in case logging method should be logged.
   */
  public boolean isLogMethod() {
    return logMethod;
  }

  /**
   * True in case logging class should be logged.
   *
   * @return true in case logging class should be logged.
   */
  public boolean isLogClass() {
    return logClass;
  }

  /**
   * True in case logger name should be logged.
   *
   * @return true in case logger name should be logged.
   */
  public boolean isLogLogger() {
    return logLogger;
  }

  /**
   * True in case MDC keys and values should be logged.
   *
   * @return true in case MDC keys and values should be logged.
   */
  public boolean isLogMdcValues() {
    return logMdcValues;
  }

  /**
   * True in case NDC should be logged.
   *
   * @return true in case NDC should be logged.
   */
  public boolean isLogNdc() {
    return logNdc;
  }

  /**
   * The external fields additionary send by logger.
   *
   * @return the external fields used during logging.
   */
  public Map<String, String> getExternalFields() {
    return externalFields;
  }
}
