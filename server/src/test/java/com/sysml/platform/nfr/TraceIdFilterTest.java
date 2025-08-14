package com.sysml.platform.nfr;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {
  @Test
  void shouldAddTraceIdIfMissing() throws Exception {
    TraceIdFilter filter = new TraceIdFilter();
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, new MockFilterChain());
    assertNotNull(res.getHeader("X-Trace-Id"));
  }
}

