package org.graylog2.gelf4j.transmitters;

import java.io.Closeable;
import java.io.IOException;

import org.graylog2.gelf4j.message.Payload;

/**
 * Transmitter for messages to Greylog server.
 * <p/>
 * Provided implementations are {@link TcpTransmitter} and {@link UdpTransmitter}.
 *
 * @author Andrey Minov
 */
public interface PayloadTransmitter extends Closeable {
  /**
   * Serialize and transmit payload message to Greylog2 server.
   *
   * @param payload the payload message that will be send to the server.
   * @throws Exception the exception
   */
  void transmit(Payload payload) throws Exception;

  /**
   * Open payload transmitter, prepare it for trasmitting.
   *
   * @throws IOException when IO operation falls. For example connection to remove server.
   */
  default void open() throws IOException {
  }
}
