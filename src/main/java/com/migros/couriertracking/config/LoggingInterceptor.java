package com.migros.couriertracking.config;

import com.migros.couriertracking.util.LoggerUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;


@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final LoggerUtil logger = LoggerUtil.of(LoggingInterceptor.class);

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_METHOD = "method";
    private static final String MDC_URI = "uri";
    private static final String ATTR_START_TIME = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String requestId = generateRequestId();

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_METHOD, request.getMethod());
        MDC.put(MDC_URI, request.getRequestURI());

        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());

        response.setHeader("X-Request-Id", requestId);

        logger.info("Incoming request: {} {} [requestId={}]",
                request.getMethod(), request.getRequestURI(), requestId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            long startTime = (long) request.getAttribute(ATTR_START_TIME);
            long duration = System.currentTimeMillis() - startTime;

            logger.info("Completed request: {} {} - status={} duration={}ms",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duration);

            if (ex != null) {
                logger.error("Request failed with exception", ex);
            }
        } finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_METHOD);
            MDC.remove(MDC_URI);
        }
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
