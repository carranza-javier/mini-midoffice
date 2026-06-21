package com.jcarranza.minimidoffice.integration.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jcarranza.minimidoffice.integration.port.GdsFlightCheckPort;
import com.jcarranza.minimidoffice.integration.port.GdsFlightSearchPort;
import com.jcarranza.minimidoffice.integration.sabre.SabreFlightCheckAdapter;
import com.jcarranza.minimidoffice.integration.sabre.SabreFlightSearchAdapter;
import com.jcarranza.minimidoffice.integration.sabre.auth.SabreOAuthProvider;
import com.jcarranza.minimidoffice.integration.sabre.client.SabreHttpClient;
import com.jcarranza.minimidoffice.integration.sabre.mapper.SabreFlightCheckMapper;
import com.jcarranza.minimidoffice.integration.sabre.mapper.SabreFlightSearchMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring configuration for the integration layer.
 *
 * To replace Sabre with Amadeus or Galileo in the future:
 *   1. Create AmadeusFlightSearchAdapter and AmadeusFlightCheckAdapter.
 *   2. Change the two @Bean methods for GdsFlightSearchPort and GdsFlightCheckPort to return the new adapters.
 *   3. Services (SearchService, BookingService) require no changes.
 */
@Configuration
@PropertySource("classpath:sabre.properties")
public class IntegrationConfig {

    /**
     * Dedicated ObjectMapper for Sabre serialization.
     * Kept as a separate bean from the Spring MVC ObjectMapper to avoid configuration conflicts.
     */
    @Bean("sabreObjectMapper")
    public ObjectMapper sabreObjectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean
    public SabreHttpClient sabreHttpClient(
            @Value("${sabre.baseUrl}") String baseUrl,
            @Value("${sabre.connectTimeoutSeconds:5}") int connectTimeout,
            @Value("${sabre.readTimeoutSeconds:30}") int readTimeout,
            ObjectMapper sabreObjectMapper) {
        return new SabreHttpClient(baseUrl, connectTimeout, readTimeout, sabreObjectMapper);
    }

    @Bean
    public SabreOAuthProvider sabreOAuthProvider(
            @Value("${sabre.tokenUrl}") String tokenUrl,
            @Value("${sabre.clientId}") String clientId,
            @Value("${sabre.clientSecret}") String clientSecret,
            SabreHttpClient sabreHttpClient) {
        return new SabreOAuthProvider(tokenUrl, clientId, clientSecret, sabreHttpClient);
    }

    @Bean
    public GdsFlightSearchPort gdsFlightSearchPort(SabreHttpClient sabreHttpClient,
                                                    SabreOAuthProvider sabreOAuthProvider,
                                                    SabreFlightSearchMapper sabreFlightSearchMapper) {
        return new SabreFlightSearchAdapter(sabreHttpClient, sabreOAuthProvider, sabreFlightSearchMapper);
    }

    @Bean
    public GdsFlightCheckPort gdsFlightCheckPort(SabreHttpClient sabreHttpClient,
                                                  SabreOAuthProvider sabreOAuthProvider,
                                                  SabreFlightCheckMapper sabreFlightCheckMapper) {
        return new SabreFlightCheckAdapter(sabreHttpClient, sabreOAuthProvider, sabreFlightCheckMapper);
    }
}
