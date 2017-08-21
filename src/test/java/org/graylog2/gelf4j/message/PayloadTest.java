package org.graylog2.gelf4j.message;

import static org.junit.Assert.assertEquals;

import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link Payload} serialization and testing.
 */
public class PayloadTest {

  private static final long TIME = 1502882757222L;
  private static final String EXPECTED =
      "{\"version\":\"1.1\",\"host\":\"localhost\",\"short_message\":\"DEBUG 0816-0930:37,740"
      + " Schedule timeout with id 10000229 for 300000ms [TTTT]\","
      + "\"full_message\":\"DEBUG 0816-0930:37,740 Schedule timeout"
      + " with id 10000229 for 300000ms [TTTT]\",\"timestamp\":1.502882757222E9,\"level\":7,"
      + "\"facility\":\"appender\",\"line\":122,\"file\":\"Message.java\",\"_application\":"
      + "\"cool-java-application\",\"_thread\":\"TTTT\",\"_version\":\"1.0.1\"}";

  private Payload payload;

  @Before
  public void initPayload() {
    payload = new Payload();
    payload.setVersion("1.1");
    payload.setHost("localhost");
    payload.setShortMessage(
        "DEBUG 0816-0930:37,740 Schedule timeout with id 10000229 for 300000ms [TTTT]");
    payload.setFullMessage(
        "DEBUG 0816-0930:37,740 Schedule timeout with id 10000229 for 300000ms [TTTT]");
    payload.setTimestamp(TIME / 1000d);
    payload.setLevel(7);
    payload.setFacility("appender");
    payload.setFile("Message.java");
    payload.setLine(122);
    payload.addAdditionalField("thread", "TTTT");
    payload.addAdditionalField("application", "cool-java-application");
    payload.addAdditionalField("version", "1.0.1");
    //
    JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
    JsonStream.registerNativeEncoder(Payload.class, new PayloadEncoder());
    JsonStream.registerNativeEncoder(StringBuilder.class, new StringBuilderEncoder());
  }

  @Test
  public void jsoniterSerialize() {
    assertEquals(EXPECTED, JsonStream.serialize(payload));
  }
}