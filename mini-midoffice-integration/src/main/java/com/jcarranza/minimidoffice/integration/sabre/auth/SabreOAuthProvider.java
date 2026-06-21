package com.jcarranza.minimidoffice.integration.sabre.auth;

import com.jcarranza.minimidoffice.domain.exception.IntegrationException;
import com.jcarranza.minimidoffice.integration.sabre.client.SabreHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Manages the Sabre OAuth2 token lifecycle.
 *
 * Sabre uses an unusual credential encoding:
 *   Authorization: Basic base64( base64(CLIENT_ID) + ":" + base64(CLIENT_SECRET) )
 *
 * The token lasts 7 days (604 800 s). It is cached in memory with a 60 s safety buffer
 * to avoid using a nearly-expired token. Thread-safe via synchronized.
 *
 * If DEVCENTER credentials expire, Sabre returns "Wrong clientID or clientSecret".
 * Regenerate them at https://developer.sabre.com/my-account/applications
 */
@Component
public class SabreOAuthProvider {

    private static final Logger log = LoggerFactory.getLogger(SabreOAuthProvider.class);
    private static final long   TOKEN_EXPIRY_BUFFER_SECONDS = 60;

    private final String         tokenUrl;
    private final String         encodedCredential;
    private final SabreHttpClient httpClient;

    private volatile String  cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public SabreOAuthProvider(
            @Value("${sabre.tokenUrl}") String tokenUrl,
            @Value("${sabre.clientId}") String clientId,
            @Value("${sabre.clientSecret}") String clientSecret,
            SabreHttpClient httpClient) {
        this.tokenUrl          = tokenUrl;
        this.encodedCredential = buildDoubleBase64Credential(clientId, clientSecret);
        this.httpClient        = httpClient;
    }

    public synchronized String getValidToken() {
        if (cachedToken == null || Instant.now().isAfter(tokenExpiry)) {
            log.info("Sabre token expired or missing — refreshing");
            refresh();
        }
        return cachedToken;
    }

    /** Forces a token refresh (useful when a call returns 401). */
    public synchronized void invalidate() {
        log.warn("Sabre token invalidated — will refresh on next call");
        this.cachedToken = null;
        this.tokenExpiry = Instant.EPOCH;
    }

    private void refresh() {
        try {
            SabreTokenResponse response = httpClient.fetchToken(tokenUrl, encodedCredential);
            this.cachedToken = response.getAccessToken();
            this.tokenExpiry = Instant.now()
                                      .plusSeconds(response.getExpiresIn() - TOKEN_EXPIRY_BUFFER_SECONDS);
            log.info("Sabre token refreshed — expires at {}", tokenExpiry);
        } catch (Exception e) {
            throw new IntegrationException("SABRE", "Failed to obtain OAuth token: " + e.getMessage(), e);
        }
    }

    /**
     * Implements the double Base64 encoding required by Sabre:
     *   credential = base64( base64(clientId) + ":" + base64(clientSecret) )
     *
     * "Credentials are missing or the syntax is not correct" = wrong encoding (plain Base64).
     * "Wrong clientID or clientSecret"                       = correct encoding, invalid credentials.
     */
    static String buildDoubleBase64Credential(String clientId, String clientSecret) {
        Base64.Encoder enc    = Base64.getEncoder();
        String         b64Id  = enc.encodeToString(clientId.getBytes(StandardCharsets.UTF_8));
        String         b64Sec = enc.encodeToString(clientSecret.getBytes(StandardCharsets.UTF_8));
        return enc.encodeToString((b64Id + ":" + b64Sec).getBytes(StandardCharsets.UTF_8));
    }
}
