package com.jcarranza.minimidoffice.integration.sabre.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcarranza.minimidoffice.domain.exception.IntegrationException;
import com.jcarranza.minimidoffice.integration.sabre.auth.SabreTokenResponse;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Shared HTTP client for all calls to the Sabre API.
 *
 * Responsibilities:
 *  - Manage the Apache HttpClient 5 CloseableHttpClient with configured timeouts.
 *  - Serialize request bodies to JSON and deserialize responses.
 *  - Log requests/responses to aid GDS error diagnosis.
 *  - Translate HTTP errors to IntegrationException with the HTTP status code.
 *
 * Base endpoint: https://api.cert.platform.sabre.com (cert) or https://api.platform.sabre.com (prod).
 */
@Component
public class SabreHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SabreHttpClient.class);

    private final String              baseUrl;
    private final ObjectMapper        objectMapper;
    private final CloseableHttpClient httpClient;

    public SabreHttpClient(
            @Value("${sabre.baseUrl}") String baseUrl,
            @Value("${sabre.connectTimeoutSeconds:5}") int connectTimeoutSeconds,
            @Value("${sabre.readTimeoutSeconds:30}") int readTimeoutSeconds,
            ObjectMapper sabreObjectMapper) {
        this.baseUrl      = baseUrl;
        this.objectMapper = sabreObjectMapper;

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(connectTimeoutSeconds))
            .setResponseTimeout(Timeout.ofSeconds(readTimeoutSeconds))
            .build();

        this.httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .build();
    }

    /**
     * Fetches an OAuth2 token via POST form-urlencoded with Basic Auth (double Base64).
     * Content-Type: application/x-www-form-urlencoded
     * Body: grant_type=client_credentials
     */
    public SabreTokenResponse fetchToken(String tokenUrl, String base64Credential) {
        HttpPost request = new HttpPost(tokenUrl);
        request.setHeader("Authorization", "Basic " + base64Credential);
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setEntity(new UrlEncodedFormEntity(
            List.of(new BasicNameValuePair("grant_type", "client_credentials")),
            StandardCharsets.UTF_8
        ));

        log.debug("Sabre token request → POST {}", tokenUrl);
        return execute(request, SabreTokenResponse.class, tokenUrl);
    }

    /**
     * Executes a JSON POST with a Bearer token.
     *
     * @param path         relative path, e.g. "/v1/offers/flightCheck"
     * @param requestBody  object to serialize as the JSON body
     * @param responseType target class for deserialization
     * @param bearerToken  OAuth2 token obtained from SabreOAuthProvider
     */
    public <T> T post(String path, Object requestBody, Class<T> responseType, String bearerToken) {
        String url = baseUrl + path;
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            log.debug("Sabre → POST {} body={}", path, json);

            HttpPost request = new HttpPost(url);
            request.setHeader("Authorization", "Bearer " + bearerToken);
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            return execute(request, responseType, path);
        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new IntegrationException("SABRE", "Failed to serialize request for " + path, e);
        }
    }

    private <T> T execute(HttpPost request, Class<T> responseType, String label) {
        long startMs = System.currentTimeMillis();
        log.info("SABRE_REQUEST → POST {}", label);
        try {
            return httpClient.execute(request, response -> {
                long   durationMs = System.currentTimeMillis() - startMs;
                int    status     = response.getCode();
                String body       = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (status < 200 || status >= 300) {
                    log.error("SABRE_ERROR ← POST {} HTTP {} ({}ms) body={}",
                        label, status, durationMs, abbreviate(body, 300));
                    throw new IntegrationException("SABRE", status,
                        "Unexpected response from " + label + ": " + abbreviate(body, 200));
                }

                log.info("SABRE_RESPONSE ← POST {} HTTP {} ({}ms)", label, status, durationMs);
                log.debug("SABRE_RESPONSE_BODY ← {} body={}", label, abbreviate(body, 500));
                return objectMapper.readValue(body, responseType);
            });
        } catch (IntegrationException e) {
            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.error("SABRE_IO_ERROR ← POST {} ({}ms): {}", label, durationMs, e.getMessage(), e);
            throw new IntegrationException("SABRE", "I/O error calling " + label, e);
        }
    }

    private static String abbreviate(String s, int maxLen) {
        if (s == null) return "(null)";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
