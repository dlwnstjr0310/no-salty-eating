package com.sparta.product.infrastructure.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReqResLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ReqResLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        MDC.put("traceId", UUID.randomUUID().toString());

        final ContentCachingRequestWrapper cachingRequestWrapper = new ContentCachingRequestWrapper(request);
        final ContentCachingResponseWrapper contentCachingResponseWrapper = new ContentCachingResponseWrapper(response);

        logger.info("Request Method: {}", cachingRequestWrapper.getMethod());
        logger.info("Request URL: {}", cachingRequestWrapper.getRequestURL());

        StringBuilder headers = new StringBuilder();
        cachingRequestWrapper.getHeaderNames().asIterator().forEachRemaining(headerName ->
                headers.append(headerName).append(": ").append(cachingRequestWrapper.getHeader(headerName)).append("\n"));
        logger.info("Request Headers:\n{}", headers);

//        String requestBody = new String(cachingRequestWrapper.getContentAsByteArray(), cachingRequestWrapper.getCharacterEncoding());
//        logger.info("Request Body: \n{}", requestBody);

        filterChain.doFilter(cachingRequestWrapper, contentCachingResponseWrapper);

        String requestBody = new String(cachingRequestWrapper.getContentAsByteArray(), cachingRequestWrapper.getCharacterEncoding());
        logger.info("Request Body: \n{}", requestBody);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // 줄바꿈 및 들여쓰기 활성화

        String responseBody = new String(contentCachingResponseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

        logger.info("Response Status: {}", contentCachingResponseWrapper.getStatus());
        logger.info("Response Header - Authorization: {}", contentCachingResponseWrapper.getHeader("Authorization"));

        if (responseBody.isEmpty()) {
            logger.info("Response body is empty");
            MDC.clear();

            return;
        } else if (responseBody.contains("swagger-ui")) {
            logger.info("swagger response");
            contentCachingResponseWrapper.copyBodyToResponse();
            MDC.clear();

            return;
        }

        Object json = objectMapper.readValue(responseBody, Object.class);
        String prettyResponseBody = objectMapper.writeValueAsString(json);

        logger.info("Response Content: \n{}", prettyResponseBody);

        contentCachingResponseWrapper.copyBodyToResponse();

        MDC.clear();
    }
}
