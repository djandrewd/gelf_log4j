package org.graylog2.gelf4j.utils;

import java.util.Map;
import java.util.regex.Pattern;

import org.graylog2.gelf4j.message.Payload;

/**
 * Utility class for validation of messages and properties.
 * Introduced to reduce amount of dependencies.
 *
 * @author Andrey Minov
 */
public final class Validations {
  private static final Pattern ADDITIONAL_PARAMETER_PATTERN = Pattern.compile("^[\\w\\.\\-]*$");

  /**
   * Check that GELF message payload is valid.
   *
   * @param payload the GELF message payload
   * @return true in case all requiments are met and false otherwise
   */
  public static boolean checkValid(Payload payload) {
    return !isEmpty(payload.getHost()) && isValidShortMessage(payload) && !isEmpty(
        payload.getVersion()) && checkNames(payload);
  }

  /**
   * Checks is string is not empty or null.
   *
   * @param str the string value to test.
   * @return the true in case tested string null or empty.
   */
  public static boolean isEmpty(CharSequence str) {
    return str == null || str.length() == 0;
  }

  private static boolean isValidShortMessage(Payload payload) {
    return !isEmpty(payload.getShortMessage()) || (payload.getShortMessageBuffer() != null
                                                   && payload.getShortMessageBuffer().limit() > 0);
  }

  private static boolean checkNames(Payload payload) {
    Map<String, String> fs = payload.getAdditionalFields();
    return fs == null || fs.isEmpty() || fs.keySet().stream()
                                           .allMatch(ADDITIONAL_PARAMETER_PATTERN.asPredicate());
  }
}
