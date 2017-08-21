package org.graylog2.gelf4j.transmitters;

import static java.util.Arrays.copyOfRange;
import static org.graylog2.gelf4j.Constants.MAX_UDP_DATAGRAM_SIZE;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Deflater;

import com.jsoniter.output.JsonStream;
import org.graylog2.gelf4j.message.Payload;

/**
 * UDP transmitter send datagrams over network.
 * </p>
 * UDP datagrams are usually limited to a size of 8192 bytes.
 * A lot of compressed information fits in there but you sometimes might just have more information to send.
 * This is why Graylog supports chunked GELF.
 * <p>
 * <h3>Chunking<a class="headerlink" href="#chunking" title="Permalink to this headline"></a></h3>
 * <p>
 * <p>UDP datagrams are usually limited to a size of 8192 bytes.
 * A lot of compressed information fits in there but you sometimes might just have
 * more information to send. This is why Graylog supports chunked GELF.</p>
 * <p>You can define chunks of messages by prepending a byte header to a GELF message including
 * a message ID and sequence number to reassemble the message later.
 * </p>
 * <p>Most GELF libraries support chunking transparently and will detect
 * if a message is too big to be sent in one datagram.
 * </p>
 * <p>Of course TCP would solve this problem on a transport
 * layer but it brings other problems that are even harder to tackle:
 * You would have to care about slow connections, timeouts and other nasty network problems.
 * </p>
 * <p>With UDP you may just lose a message while with TCP
 * it could bring your whole application down when not designed with care.
 * </p>
 * <p>Of course TCP makes sense in some (especially high volume environments)
 * so it is your decision. Many GELF libraries support both TCP and UDP as transport. Some
 * do even support HTTP.
 * </p>
 * <p>
 * Prepend the following structure to your GELF message to make it chunked:
 * </p>
 * <blockquote>
 * <div><ul class="simple">
 * <li><strong>Chunked GELF magic bytes - 2 bytes:</strong> <code class="docutils literal">
 * <span class="pre">0x1e</span><span class="pre">0x0f</span></code></li>
 * <li><strong>Message ID - 8 bytes:</strong> Must be the same for every chunk of this message.
 * Identifying the whole message and is used to reassemble the chunks later. Generate from
 * millisecond timestamp + hostname for example.</li>
 * <li><strong>Sequence number - 1 byte:</strong>
 * The sequence number of this chunk. Starting at 0 and always less than the sequence count.</li>
 * <li><strong>Sequence count - 1 byte:</strong>
 * Total number of chunks this message has.</li>
 * </ul>
 * </div></blockquote>
 * <p>All chunks <strong>MUST</strong> arrive within 5 seconds or the server
 * will discard all already arrived and still arriving chunks.
 * A message <strong>MUST NOT</strong> consist of more than 128 chunks.</p>
 * </div>
 * <p>
 * <p>
 * <div class="section" id="compression">
 * <h3>
 * Compression<a class="headerlink" href="#compression" title="Permalink to this headline"></a>
 * </h3>
 * <p>When using UDP as transport layer,
 * GELF messages can be sent uncompressed or compressed with either GZIP or ZLIB.
 * </p>
 * <p>Graylog nodes detect the compression type in the GELF magic byte header automatically.
 * </p>
 * <p>Decide if you want to trade a bit more CPU load for saving a lot of network bandwidth.
 * GZIP is the protocol default.
 * </p>
 *
 * @author Andrey Minov
 */
public class UdpTransmitter implements PayloadTransmitter, Closeable {

  private static final byte[] GELF_CHUNKED_ID = new byte[] {0x1e, 0x0f};
  private static final int MESSAGE_ID_SIZE = 8;
  private static final int CHUNK_SIZE =
      MAX_UDP_DATAGRAM_SIZE - GELF_CHUNKED_ID.length - MESSAGE_ID_SIZE - 2;
  /**
   * Static Deflater / Inflater are used as they are use native libraries and consume SYSTEM memory (not
   * Heap)
   * <p>
   * As Packager is usually created for each Serializer/Session many serializers cause JVM to consume lot of
   * system memory (5k sessions - 5Gb RAM)
   * <p>
   * Also they are not thread safe so MUST be thread local / synchronized.
   */
  private final static ThreadLocal<Deflater> tlhDeflater = ThreadLocal.withInitial(Deflater::new);
  private final static ThreadLocal<SecureRandom> thSecureRandom =
      ThreadLocal.withInitial(SecureRandom::new);
  private static final Charset CHARSET = Charset.forName("UTF-8");

  private final String hostname;
  private final int port;
  private final int sendBufferSize;
  private final boolean useCompression;
  private final int compressionLevel;
  private final int compressionLimit;
  private DatagramChannel channel;
  private Lock lock;

  /**
   * Instantiates a new UDP transmitter. It uses always NIO.
   *
   * @param hostname         the hostname of the Graylog server
   * @param port             the port of the server.
   * @param sendBufferSize   the send buffer size in bytes.
   *                         -1 in case of default.
   * @param useCompression   true if compressioon must be used, default false.
   * @param compressionLevel ZLIB level of compression from 1 to 9. Default is 5.
   * @param compressionLimit size in bytes after which compression must be applied - default to 4096.
   */
  public UdpTransmitter(String hostname, int port, int sendBufferSize, boolean useCompression,
                        int compressionLevel, int compressionLimit) {
    this.hostname = hostname;
    this.port = port;
    this.sendBufferSize = sendBufferSize;
    this.useCompression = useCompression;
    this.compressionLevel = compressionLevel;
    this.compressionLimit = compressionLimit;
    this.lock = new ReentrantLock();
  }

  private static byte[] compress(byte[] data, int compressionLevel) {
    byte[] compressedData = new byte[0];

    Deflater deflater = tlhDeflater.get();
    deflater.setLevel(compressionLevel);
    deflater.setInput(data);
    deflater.finish();

    while (!deflater.finished()) {
      byte[] compressed = new byte[data.length];
      int outSize = deflater.deflate(compressed);

      byte[] newCompressed = new byte[compressedData.length + outSize];
      System.arraycopy(compressedData, 0, newCompressed, 0, compressedData.length);
      System.arraycopy(compressed, 0, newCompressed, compressedData.length, outSize);
      compressedData = newCompressed;
    }
    deflater.reset();

    return compressedData;
  }

  @Override
  public void open() throws IOException {
    lock.lock();
    try {
      SocketAddress address = new InetSocketAddress(hostname, port);
      channel = DatagramChannel.open();
      channel.socket().bind(new InetSocketAddress(0));
      channel.connect(address);
      // Make UDP blocking makes not sence.
      channel.configureBlocking(false);
      if (sendBufferSize > 0) {
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
      // In case you use log4j2 JsonStream instances is reused by ThreadLocal and not created every time.
      String message = JsonStream.serialize(payload);
      byte[] bytes = message.getBytes(CHARSET);
      if (useCompression && bytes.length > compressionLimit) {
        bytes = compress(bytes, compressionLevel);
      }

      if (bytes.length <= MAX_UDP_DATAGRAM_SIZE) {
        channel.write(ByteBuffer.wrap(bytes));
        return;
      }

      int chunks = bytes.length / CHUNK_SIZE + 1;
      if (chunks > Byte.MAX_VALUE) {
        return;
      }

      byte[] messageId = new byte[MESSAGE_ID_SIZE];
      thSecureRandom.get().nextBytes(messageId);
      ByteBuffer buffer = ByteBuffer.allocate(CHUNK_SIZE);

      for (int i = 0; i < chunks; i++) {
        int dataStart = i * CHUNK_SIZE;
        int dataEnd = Math.min((i + 1) * CHUNK_SIZE, bytes.length);
        buffer.put(GELF_CHUNKED_ID);
        buffer.put(messageId);
        buffer.put((byte) i);
        buffer.put((byte) chunks);
        buffer.put(copyOfRange(bytes, dataStart, dataEnd));
        buffer.flip();
        channel.write(buffer);
        buffer.clear();
      }
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
