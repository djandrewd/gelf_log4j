package org.graylog2.gelf4j.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.graylog2.gelf4j.message.Payload;
import org.junit.Test;

/**
 * Tests for validation checks.
 *
 * @author Andrey Minov
 */
public class ValidationsTest {

  @Test
  public void testEmpty() throws Exception {
    assertTrue(Validations.isEmpty(null));
    assertTrue(Validations.isEmpty(""));
  }

  @Test
  public void testNonEmpty() throws Exception {
    assertFalse(Validations.isEmpty("hello!"));
    assertFalse(Validations.isEmpty("   "));
  }

  @Test
  public void testMissingName() {
    Payload payload = new Payload();
    assertFalse(Validations.checkValid(payload));
  }

  @Test
  public void testMissingHost() {
    Payload payload = new Payload();
    payload.setShortMessage("msg");
    assertFalse(Validations.checkValid(payload));
  }

  @Test
  public void testMissingVersion() {
    Payload payload = new Payload();
    payload.setShortMessage("msg");
    payload.setHost("localhost");
    assertFalse(Validations.checkValid(payload));
  }

  @Test
  public void testExtraNamesIncorrect() {
    Payload payload = new Payload();
    payload.setShortMessage("msg");
    payload.setHost("localhost");
    payload.setVersion("1.1");
    payload.addAdditionalField("~{}}{[", "extra");
    assertFalse(Validations.checkValid(payload));
  }

  @Test
  public void testPayloadIncorrect() {
    Payload payload = new Payload();
    payload.setShortMessage("msg");
    payload.setHost("localhost");
    payload.setVersion("1.1");
    payload.addAdditionalField("extra", "extra");
    assertTrue(Validations.checkValid(payload));
  }

}