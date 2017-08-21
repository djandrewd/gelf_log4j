package org.graylog2.gelf4j.message;

import java.io.IOException;

import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.Encoder;

/**
 * JSON encoder for String Builder.
 *
 * @author Andrey Minov
 */
public class StringBuilderEncoder implements Encoder {
  @Override
  public void encode(Object obj, JsonStream stream) throws IOException {
    StringBuilder builder = (StringBuilder) obj;
    stream.writeVal(builder.toString());
  }

  @Override
  public Any wrap(Object obj) {
    return Any.wrap(obj);
  }
}
