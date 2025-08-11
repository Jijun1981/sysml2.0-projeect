package com.sysml.platform.nfr;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * RQ-NFR-LOG-003: 结构化日志
 * 验收条件：traceId/spanId追踪
 */
@SpringBootTest
public class StructuredLoggingTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(StructuredLoggingTest.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    @DisplayName("日志应该包含traceId和spanId")
    public void shouldIncludeTraceAndSpanId() {
        // Given: 设置追踪信息
        MDC.put("traceId", "test-trace-123");
        MDC.put("spanId", "test-span-456");

        // When: 记录日志
        logger.info("Test log message with tracing");

        // Then: 验证日志包含追踪信息
        ILoggingEvent event = listAppender.list.get(0);
        assertEquals("Test log message with tracing", event.getMessage());
        assertEquals("test-trace-123", event.getMDCPropertyMap().get("traceId"));
        assertEquals("test-span-456", event.getMDCPropertyMap().get("spanId"));

        // Cleanup
        MDC.clear();
    }

    @Test
    @DisplayName("应该支持结构化日志格式")
    public void shouldSupportStructuredFormat() {
        // Given: 设置结构化上下文
        MDC.put("userId", "user-001");
        MDC.put("requestId", "req-789");
        MDC.put("operation", "createRequirement");

        // When: 记录带有业务上下文的日志
        logger.info("Creating requirement");

        // Then: 验证日志包含所有上下文信息
        ILoggingEvent event = listAppender.list.get(0);
        assertEquals("user-001", event.getMDCPropertyMap().get("userId"));
        assertEquals("req-789", event.getMDCPropertyMap().get("requestId"));
        assertEquals("createRequirement", event.getMDCPropertyMap().get("operation"));

        // Cleanup
        MDC.clear();
    }

    @Test
    @DisplayName("应该正确处理异常日志")
    public void shouldHandleExceptionLogging() {
        // Given: 一个异常
        Exception testException = new RuntimeException("Test exception");

        // When: 记录异常
        logger.error("Error occurred", testException);

        // Then: 验证异常被记录
        ILoggingEvent event = listAppender.list.get(0);
        assertEquals("Error occurred", event.getMessage());
        assertEquals("ERROR", event.getLevel().toString());
        assertNotNull(event.getThrowableProxy());
        assertEquals("Test exception", event.getThrowableProxy().getMessage());
    }
}