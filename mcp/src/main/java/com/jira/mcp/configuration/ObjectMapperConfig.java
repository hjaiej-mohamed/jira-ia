package com.jira.mcp.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for customizing the Jackson {@link ObjectMapper}.
 *
 * <p>This configuration provides a primary ObjectMapper bean that is used throughout
 * the application for JSON serialization and deserialization. It is tailored to handle:
 * <ul>
 *     <li>Java 8 date/time types such as {@link java.time.Instant} and {@link java.time.LocalDateTime}</li>
 *     <li>Optional types from {@link java.util.Optional} and other JDK8 types</li>
 *     <li>Ignoring unknown JSON properties during deserialization to prevent failures</li>
 *     <li>Writing dates in ISO-8601 format instead of timestamps</li>
 * </ul>
 *
 * <p>All components that require an {@link ObjectMapper} (e.g., REST controllers, repositories,
 * or tools) will automatically inject this configured bean.
 */
@Configuration
public class ObjectMapperConfig {

    /**
     * Provides the primary {@link ObjectMapper} for the application.
     *
     * @return Configured ObjectMapper with JavaTimeModule, Jdk8Module,
     *         and disabled WRITE_DATES_AS_TIMESTAMPS and FAIL_ON_UNKNOWN_PROPERTIES features
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())           // Support for Instant, LocalDateTime, etc.
                .registerModule(new Jdk8Module())               // Support for Optional, Long, etc.
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // Use ISO-8601 instead of numeric timestamps
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // Ignore unknown JSON properties
    }
}
