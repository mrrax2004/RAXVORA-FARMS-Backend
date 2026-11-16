package com.dairyfarm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigTest {
    @Test
    void parsesCommaSeparatedCorsOriginsAsSeparateBrowserOrigins() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        var source = new SecurityConfig().corsConfigurationSource(
                "http://localhost:5173, http://192.168.1.7:5173", environment);
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/dashboard"));

        assertEquals(List.of("http://localhost:5173", "http://192.168.1.7:5173"), configuration.getAllowedOrigins());
        assertTrue(configuration.checkOrigin("http://localhost:5173") != null);
        assertTrue(configuration.checkOrigin("http://192.168.1.7:5173") != null);
    }

    @Test
    void rejectsWildcardCorsOriginsInProduction() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        assertThrows(IllegalArgumentException.class,
                () -> new SecurityConfig().corsConfigurationSource("*", environment));
    }
}
