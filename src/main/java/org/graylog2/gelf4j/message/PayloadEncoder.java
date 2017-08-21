package org.graylog2.gelf4j.message;

import static org.graylog2.gelf4j.Constants.FACILITY;
import static org.graylog2.gelf4j.Constants.FILE;
import static org.graylog2.gelf4j.Constants.FULL_MESSAGE;
import static org.graylog2.gelf4j.Constants.HOST;
import static org.graylog2.gelf4j.Constants.LEVEL;
import static org.graylog2.gelf4j.Constants.LINE;
import static org.graylog2.gelf4j.Constants.SHORT_MESSAGE;
import static org.graylog2.gelf4j.Constants.TIMESTAMP;
import static org.graylog2.gelf4j.Constants.VERSION;
import static org.graylog2.gelf4j.utils.Validations.isEmpty;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.Encoder;

/**
 * Jsoniter {@link com.jsoniter.spi.Encoder} for writing GELF message.
 *
 * @author Andrey Minov
 */
public class PayloadEncoder implements Encoder {
  private static final byte VALUE = '"';

  @Override
  public void encode(Object obj, JsonStream stream) throws IOException {
    Payload payload = (Payload) obj;
    stream.writeObjectStart();
    //version
    stream.writeObjectField(VERSION);
    stream.writeVal(payload.getVersion());
    stream.writeMore();
    // host
    stream.writeObjectField(HOST);
    stream.writeVal(payload.getHost());
    stream.writeMore();
    // short_message
    stream.writeObjectField(SHORT_MESSAGE);
    ByteBuffer messageBuffer = payload.getShortMessageBuffer();
    if (messageBuffer != null && messageBuffer.limit() > 0) {
      stream.write(VALUE);
      byte[] array = messageBuffer.array();
      for (int i = 0; i < messageBuffer.limit(); i++) {
        stream.write(array[i]);
      }
      stream.write(VALUE);
    } else {
      stream.writeVal(payload.getShortMessage());
    }
    // full_message
    CharSequence fullMessage = payload.getFullMessage();
    if (!isEmpty(fullMessage)) {
      stream.writeMore();
      stream.writeObjectField(FULL_MESSAGE);
      stream.writeVal(fullMessage);
    }
    // timestamp
    if (payload.getTimestamp() > 0) {
      stream.writeMore();
      stream.writeObjectField(TIMESTAMP);
      stream.writeVal(payload.getTimestamp());
    }
    // level
    if (payload.getLevel() > 0) {
      stream.writeMore();
      stream.writeObjectField(LEVEL);
      stream.writeVal(payload.getLevel());
    }
    // facility
    CharSequence facility = payload.getFacility();
    if (!isEmpty(facility)) {
      stream.writeMore();
      stream.writeObjectField(FACILITY);
      stream.writeVal(facility);
    }
    // line
    if (payload.getLine() > 0) {
      stream.writeMore();
      stream.writeObjectField(LINE);
      stream.writeVal(payload.getLine());
    }
    // file
    CharSequence file = payload.getFile();
    if (!isEmpty(file)) {
      stream.writeMore();
      stream.writeObjectField(FILE);
      stream.writeVal(file);
    }
    // additional properties
    Map<String, String> additionalFields = payload.getAdditionalFields();
    if (additionalFields != null && !additionalFields.isEmpty()) {
      for (Map.Entry<String, String> entry : additionalFields.entrySet()) {
        stream.writeMore();
        stream.writeObjectField("_" + entry.getKey());
        stream.writeVal(entry.getValue());
      }
    }
    stream.writeObjectEnd();
  }

  @Override
  public Any wrap(Object obj) {
    return Any.wrap(obj);
  }
}
