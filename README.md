## GELF log4j appender for log4j version 1 and 2.

This is library for appender to Graylog2 server in GELF format (http://docs.graylog.org/en/2.2/pages/gelf.html).

## Configuration

1. **graylogHost** - hostname of Graylog server. In format 'protocol:hostname'. At this moment 
only UDP and TCP are supported. 
Example: tcp:localhost
2. **port** - Graylog server listen port. In case not set default 12201 will be used.
3. **blocking** - true in case old blocking IO must be used. Default is false.
4. **soTimeout** - socket timeout in milliseconds in case blocking set to true. Default: 2000
5. **facility** - facility used for logging. Default : gelf4j-appender
6. **logExceptions** - true in case exception must be logged as full_message. Default: true
7. **logThread** - true in case execution thread must be logged as separate field. Default: true
8. **logLine** - true in case execution line must be logged as separate field. Default: false
9. **logFile** - true in case execution file must be logged as separate field. Default: false
10. **logMethod** - true in case execution method must be logged as separate field. Default: false
11. **logClass** - true in case execution class must be logged as separate field. Default: false
12. **logLogger**  - true in case logger name must be logged as separate field. Default: false
13. **logMdcValues** - true in case MDC fields must be logged each as separate field. Default: false
14. **sendBufferSize** - send buffer size in bytes in case non blocking IO used. Default -1 - means 
use system defaults.
15. **useCompression** - true in case GZIP compression for UDP packets will be used. Default: false
16. **compressionLevel** - compression level of the package. Default : 5
17. **compressionLimit** - mininum size of payload to apply compression. Default : 8192
18. **useCircuitBreaker** - true in case <a href="https://martinfowler.com/bliki/CircuitBreaker.html">Circuit breaker</a>
    must be used to minimize IO operations in case lost of failures.
19. **failuresToOpen** - number of IO failures needed to open circuit breaker.
20. **secondsToRecover** - seconds to recover after failures.

### Log4j 1.2

Appender for log4j version 1.2 is 

```
  org.graylog2.gelf4j.appenders.Log4jGelfAppender
```

Example configuration looks like

```properties
log4j.rootLogger=DEBUG, gelf_appender

log4j.appender.gelf_appender=org.graylog2.gelf4j.appenders.Log4jGelfAppender
log4j.appender.gelf_appender.graylogHost=tcp:localhost
log4j.appender.gelf_appender.blocking=true
log4j.appender.gelf_appender.useCircuitBreaker=true
log4j.appender.gelf_appender.useCompression=true
log4j.appender.gelf_appender.layout=org.apache.log4j.PatternLayout
log4j.appender.gelf_appender.layout.ConversionPattern=%p %d{MMdd-HHmm:ss,SSS} %-4r [%t] \
  [%X{process}] %c{1} - %m%n
```

### Log4j version 2.8

Appender for log4j version 2.8 is

```
org.graylog2.gelf4j.appenders.Log4j2GelfAppender
```

**Note!**

Hostname in version 2.8 must be set without connection type.
Use type of the protocol as :

1. **type** - connection type. One of tcp or udp.



Example configuration looks like

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="INFO">
    <Appenders>
        <Gelf4j2 name="syslogtest" hostname="localhost" type="tcp">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
            <KeyValuePair key="additionalField1" value="additional value 1"/>
        </Gelf4j2>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="syslogtest"/>
        </Root>
    </Loggers>
</Configuration>
```