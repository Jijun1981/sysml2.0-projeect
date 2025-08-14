package com.sysml.platform.nfr;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = request.getHeader("X-Trace-Id");
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
    }
    String spanId = UUID.randomUUID().toString();
    MDC.put("traceId", traceId);
    MDC.put("spanId", spanId);
    try {
      response.addHeader("X-Trace-Id", traceId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("traceId");
      MDC.remove("spanId");
    }
  }
}

