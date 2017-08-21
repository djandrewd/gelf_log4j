package org.graylog2.gelf4j.transmitters;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jsoniter.output.JsonStream;
import org.graylog2.gelf4j.message.Payload;

/**
 * TCP transmitter send message to graylog service using TCP.
 * <p>
 * <p>
 * At the current time, GELF TCP only supports uncompressed and non-chunked payloads.
 * Each message needs to be delimited with a null byte (\0) when sent in the same TCP connection.
 * </p>
 * <p>
 * <b>Attention</b>
 * </p>
 * <p>
 * GELF TCP <strong>does not support</strong> compression due to the use of the null byte
 * (\0) as frame delimiter.
 * </p>
 *
 * @author Andrey Minov
 */
public class TcpTransmitter implements PayloadTransmitter, Closeable {
  private static final Charset CHARSET = Charset.forName("UTF-8");
  private static final byte[] ENDING = "\0".getBytes(CHARSET);

  private final String hostname;
  private final int port;
  private final int soTimeout;
  private final boolean blocking;
  private final int sendBufferSize;
  private SocketChannel channel;
  private Lock lock;

  /**
   * Instantiates a new Tcp transmitter.
   *
   * @param hostname       the hostname of the Graylog server
   * @param port           the port of the server
   * @param soTimeout      the socket timeout, apply only in case channel is blocking.
   * @param blocking       true in case use blocking IO, otherwise NIO will be used.
   * @param sendBufferSize the send buffer size int bytes in case blocking IO sent to false.
   *                       -1 in case of default.
   */
  public TcpTransmitter(String hostname, int port, int soTimeout, boolean blocking,
                        int sendBufferSize) {
    this.hostname = hostname;
    this.port = port;
    this.soTimeout = soTimeout;
    this.blocking = blocking;
    this.sendBufferSize = sendBufferSize;
    this.lock = new ReentrantLock();
  }

  @Override
  public void open() throws IOException {
    lock.lock();
    try {
      SocketAddress address = new InetSocketAddress(hostname, port);
      channel = SocketChannel.open();
      channel.socket().setSoTimeout(soTimeout);
      channel.socket().connect(address, soTimeout);
      channel.configureBlocking(blocking);
      if (!blocking && sendBufferSize > 0) {
        channel.setOption(StandardSocketOptions.SO_SNDBUF, sendBufferSize);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void transmit(Payload payload) throws Exception {
    lock.lock();
    try {
      // TCP connections might be dropped suddenly without FIN flag send.
      // This mostly happends by IP tables renew and most
      // exceptions followed by 'Connection Reset by Peer.'
      if (channel == null || !channel.isOpen() || !channel.isConnected()) {
        open();
      }
      // In case you use log4j2 JsonStream instances is reused by ThreadLocal and not created every time.
      String message = JsonStream.serialize(payload);
      byte[] bytes = message.getBytes(CHARSET);
      ByteBuffer buffer = ByteBuffer.allocate(bytes.length + ENDING.length);
      buffer.put(bytes);
      buffer.put(ENDING);
      buffer.flip();
      channel.write(buffer);
    } catch (Exception e) {
      channel = null;
      throw e;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void close() throws IOException {
    lock.lock();
    try {
      if (channel != null) {
        channel.close();
      }
    } finally {
      lock.unlock();
    }
  }
}
