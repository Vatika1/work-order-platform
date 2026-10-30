package com.vatika.workorder.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Logs one line per request: method, route template, status, duration, client.
 * Requests slower than 1s are logged at WARN.
 * Runs right after CorrelationIdFilter so correlationId is still in MDC.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);
    private static final long SLOW_MS = 1000;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().contains("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String route = pattern != null ? pattern.toString() : request.getRequestURI();
            Object clientId = request.getAttribute("clientId");

            Object[] args = {
                    kv("method", request.getMethod()),
                    kv("route", route),
                    kv("status", response.getStatus()),
                    kv("durationMs", durationMs),
                    kv("clientId", clientId != null ? clientId : "none")
            };

            if (durationMs >= SLOW_MS) {
                log.warn("Slow request {} {} {} {} {}", args);
            } else {
                log.info("Request {} {} {} {} {}", args);
            }
        }
    }
}