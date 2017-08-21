package org.graylog2.gelf4j.message;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.graylog2.gelf4j.Constants;

/**
 * This is GELF payload message described in
 * <a href="http://docs.graylog.org/en/2.2/pages/gelf.html">GELF</a>
 * <p/>
 * <h2>GELF Payload Specification</h2>
 * <p>Version 1.1 (11/2013)</p>
 * <p>A GELF message is a JSON string with the following fields:</p>
 * <blockquote>
 * <div><ul>
 * <li><dl class="first docutils">
 * <dt><strong>version</strong> <code class="docutils literal"><span class="pre">string</span>
 * <span class="pre">(UTF-8)</span></code></dt>
 * <dd><ul class="first last simple">
 * <li>GELF spec version – &#8220;1.1&#8221;; <strong>MUST</strong> be set by client library.</li>
 * </ul>
 * </dd>
 * </dl>
 * </li>
 * <li><dl class="first docutils">
 * <dt><strong>host</strong> <code class="docutils literal"><span class="pre">string</span>
 * <span class="pre">(UTF-8)</span></code></dt>
 * <dd><ul class="first last simple">
 * <li>the name of the host, source or application that sent this message; <strong>MUST</strong> be set by client library.</li>
 * </ul>
 * </dd>
 * </dl>
 * </li>
 * <li><dl class="first docutils">
 * <dt><strong>short_message</strong> <code class="docutils literal"><span class="pre">string</span>
 * <span class="pre">(UTF-8)</span></code></dt>
 * <dd><ul class="first last simple">
 * <li>a short descriptive message; <strong>MUST</strong> be set by client library.</li>
 * </ul>
 * </dd>
 * </dl>
 * </li>
 * <li><dl class="first docutils">
 * <dt><strong>full_message</strong> <code class="docutils literal"><span class="pre">string</span>
 * <span class="pre">(UTF-8)</span></code></dt>
 * <dd><ul class="first last simple">
 * <li>a long message that can i.e. contain a backtrace; optional.</li>
 * </ul>
 * </dd>
 * </dl>
 * </li>
 * <li><dl class="first docutils">
 * <dt><strong>timestamp</strong> <code class="docutils literal"><span class="pre">number</span></code></dt>
 * <dd><ul class="first last simple">
 * <li>Seconds since UNIX epoch with optional decimal places for milliseconds; <em>SHOULD</em> be set by client library.
 * Will be set to the current timestamp (now) by the server if absent.</li>
 * </ul>
 * </dd>
 * </dl>
 * </li>
 * <li><dl class="first docutils">
 * <dt><strong>level</strong> <code class="docutils literal"><span class="pre">number</span></code></dt>
 * <dd><ul class="first last simple">
 * <li>the level equal to the standard syslog levels; optional, default is 1 (ALERT).</li>
 * </ul>
 * </dd>
 * </dl>
 * </li>
 * <li><dl class="first docutils">
 * <dt><strong>facility</strong> <code class="docutils literal"><span class="pre">string</span>
 * <span class="pre">(UTF-8)</span></code></dt>
 * <dd><ul class="first last simple">
 * <li>optional, deprecated. Send as additional field instead.</li>
 * </ul>
 * </dd>
 * </dl>
 * </li>
 * <li><dl class="first docutils">
 * <dt><strong>line</strong> <code class="docutils literal"><span class="pre">number</span></code></dt>
 * <dd><ul class="first last simple">
 * <li>the line in a file that caused the error (decimal); optional, deprecated. Send as additional field instead.</li>
 * </ul>
 * </dd>
 * </dl>
 * </li>
 * <li><dl class="first docutils">
 * <dt><strong>file</strong> <code class="docutils literal"><span class="pre">string</span>
 * <span class="pre">(UTF-8)</span></code></dt>
 * <dd><ul class="first last simple">
 * <li>the file (with path if you want) that caused the error (string); optional, deprecated.
 * Send as additional field instead.</li>
 * </ul>
 * </dd>
 * </dl>
 * </li>
 * <li><dl class="first docutils">
 * <dt><strong>_[additional field]</strong> <code class="docutils literal"><span class="pre">string</span>
 * <span class="pre">(UTF-8)</span></code> or <code class="docutils literal"><span class="pre">number</span></code></dt>
 * <dd><ul class="first last simple">
 * <li>every field you send and prefix with an underscore (<code class="docutils literal"><span class="pre">_</span></code>)
 * will be treated as an additional field. Allowed characters in field names are any word character (letter, number, underscore),
 * dashes and dots. The verifying regular expression is: <code class="docutils literal"><span class="pre">^[\w\.\-]*$</span></code>.
 * Libraries SHOULD not allow to send id as additional field (<code class="docutils literal"><span class="pre">_id</span></code>).
 * Graylog server nodes omit this field automatically.</li>
 * </ul>
 * </dd>
 * </dl>
 * </li>
 * </ul>
 * </div></blockquote>
 * </div>
 * <p>Example<p/>
 * <code>
 * {
 * "version": "1.1", <br>
 * "host": "example.org", <br>
 * "short_message": "A short message that helps you identify what is going on", <br>
 * "full_message": "Backtrace here\n\nmore stuff", <br>
 * "timestamp": 1385053862.3072, <br>
 * "level": 1, <br>
 * "_user_id": 9001, <br>
 * "_some_info": "foo", <br>
 * "_some_env_var": "bar" <br>
 * }
 * <p>
 * </code>
 *
 * @author Andrey Minov
 */
public class Payload {
  private StringBuilder version;
  private StringBuilder host;
  private StringBuilder shortMessage;
  private StringBuilder fullMessage;
  private double timestamp;
  private int level;
  private StringBuilder facility;
  private int line;
  private StringBuilder file;
  private Map<String, String> additionalFields;
  // In case message use bufferization we have to use buffers for message.
  private ByteBuffer shortMessageBuffer;


  public Payload() {
    this.version = new StringBuilder();
    this.host = new StringBuilder();
    this.shortMessage = new StringBuilder();
    this.fullMessage = new StringBuilder();
    this.facility = new StringBuilder();
    this.file = new StringBuilder();
  }

  public StringBuilder getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version.append(version);
  }

  public StringBuilder getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host.append(host);
  }

  public StringBuilder getShortMessage() {
    return shortMessage;
  }

  public void setShortMessage(CharSequence shortMessage) {
    this.shortMessage.append(shortMessage);
  }

  public StringBuilder getFullMessage() {
    return fullMessage;
  }

  public void setFullMessage(CharSequence fullMessage) {
    this.fullMessage.append(fullMessage);
  }

  public double getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(double timestamp) {
    this.timestamp = timestamp;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public void addAdditionalField(String name, String value) {
    if (additionalFields == null) {
      additionalFields = new HashMap<>();
    }
    additionalFields.put(name, value);
  }

  public ByteBuffer getShortMessageBuffer() {
    return shortMessageBuffer;
  }

  public void setShortMessageBuffer(byte[] shortMessageEncoded) {
    if (shortMessageBuffer == null || shortMessageEncoded.length > shortMessageBuffer.capacity()) {
      shortMessageBuffer =
          ByteBuffer.allocate(Math.max(shortMessageEncoded.length, Constants.MESSAGE_BUFFER_SIZE));
    }
    shortMessageBuffer.clear();
    shortMessageBuffer.put(shortMessageEncoded);
    shortMessageBuffer.flip();
  }

  public Map<String, String> getAdditionalFields() {
    return additionalFields;
  }

  public StringBuilder getFacility() {
    return facility;
  }

  @Deprecated
  public void setFacility(String facility) {
    this.facility.append(facility);
  }

  public int getLine() {
    return line;
  }

  @Deprecated
  public void setLine(int line) {
    this.line = line;
  }

  public StringBuilder getFile() {
    return file;
  }

  @Deprecated
  public void setFile(String file) {
    this.file.append(file);
  }

  public void clear() {
    host.setLength(0);
    shortMessage.setLength(0);
    fullMessage.setLength(0);
    timestamp = 0;
    line = 0;
    file.setLength(0);
    facility.setLength(0);
    level = 0;
    version.setLength(0);

    if (additionalFields != null) {
      additionalFields.clear();
    }
  }
}
