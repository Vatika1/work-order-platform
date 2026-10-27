package com.vatika.workorder.shared.security;

import com.vatika.workorder.shared.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse body = ErrorResponse.of(
                status.value(), status.getReasonPhrase(),
                "Authentication required", request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String json = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s","correlationId":"%s","details":[]}"""
                .formatted(
                        body.timestamp(),
                        body.status(),
                        body.error(),
                        body.message(),
                        body.path(),
                        body.correlationId() != null ? body.correlationId() : "");

        response.getWriter().write(json);
    }
}