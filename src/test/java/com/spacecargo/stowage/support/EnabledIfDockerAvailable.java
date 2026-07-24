package com.spacecargo.stowage.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.DockerClientFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables a test class only when a usable Docker environment is reachable, so
 * Testcontainers-backed tests are skipped (not failed) on machines without Docker
 * or with a Docker daemon Testcontainers cannot talk to. On CI's standard Linux
 * Docker the condition passes and the test runs.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledIfDockerAvailable.DockerAvailableCondition.class)
public @interface EnabledIfDockerAvailable {

    class DockerAvailableCondition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            try {
                if (DockerClientFactory.instance().isDockerAvailable()) {
                    return ConditionEvaluationResult.enabled("Docker is available");
                }
            } catch (Throwable ignored) {
                // fall through to disabled
            }
            return ConditionEvaluationResult.disabled("No usable Docker environment; skipping Testcontainers test");
        }
    }
}
