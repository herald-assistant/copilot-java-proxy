package com.acme.herald.copilot.core.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CopilotExceptionMapper {

    private CopilotExceptionMapper() {
    }

    public static RuntimeException mapExecutionFailure(Class<?> sourceClass, Exception e) {
        Logger logger = LoggerFactory.getLogger(sourceClass);
        logger.info(e.getMessage(), e);
        return new RuntimeException("Copilot execution failed: " + safeMsg(e), e);
    }

    public static String safeMsg(Exception e) {
        String message = e.getMessage();
        return (message == null || message.isBlank())
                ? e.getClass().getSimpleName()
                : message;
    }
}