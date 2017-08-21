package org.graylog2.gelf4j.transmitters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

import org.graylog2.gelf4j.message.Payload;
import org.junit.Before;
import org.junit.Test;

/**
 * TCP payload transmitter test.
 *
 * @author Andrey Minov
 */
public class TcpTransmitterTest {

  private static ThreadLocal<SocketChannel> channelTL =
      ThreadLocal.withInitial(() -> mock(SocketChannel.class));

  static {
    System.setProperty("java.nio.channels.spi.SelectorProvider",
        "org.graylog2.gelf4j.transmitters.TcpTransmitterTest$MockSelectorProvider");
  }

  private SocketChannel socketChannel;
  private TcpTransmitter transmitter;

  @Before
  public void init() {
    socketChannel = channelTL.get();
    Socket socket = mock(Socket.class);
    when(socketChannel.socket()).thenReturn(socket);
    transmitter = new TcpTransmitter("localhost", 1212, 100, false, -1);
  }

  @Test
  public void testReconnection() throws IOException {
    try {
      when(socketChannel.write(any(ByteBuffer.class))).thenThrow(new IOException());
    } catch (IOException e) {
      // Ignore.
    }
    for (int i = 0; i < 2; i++) {
      try {
        transmitter.transmit(new Payload());
      } catch (Exception e) {
        // Ignore
      }
    }

    SocketAddress address = new InetSocketAddress("localhost", 1212);
    verify(socketChannel.socket(), times(2)).setSoTimeout(100);
    verify(socketChannel.socket(), times(2)).connect(address, 100);
  }

  public static class MockSelectorProvider extends SelectorProvider {
    @Override
    public DatagramChannel openDatagramChannel() throws IOException {
      return mock(DatagramChannel.class);
    }

    @Override
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
      return mock(DatagramChannel.class);
    }

    @Override
    public Pipe openPipe() throws IOException {
      return mock(Pipe.class);
    }

    @Override
    public AbstractSelector openSelector() throws IOException {
      return mock(AbstractSelector.class);
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
      return mock(ServerSocketChannel.class);
    }

    @Override
    public SocketChannel openSocketChannel() throws IOException {
      return channelTL.get();
    }
  }

}