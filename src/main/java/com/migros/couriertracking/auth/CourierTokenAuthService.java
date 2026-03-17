package com.migros.couriertracking.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.couriertracking.exception.UnauthorizedRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CourierTokenAuthService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JWT_HEADER_JSON = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
    private final ObjectMapper objectMapper;

    public Long resolveCourierId(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedRequestException("Missing or invalid Bearer token.");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        String[] segments = token.split("\\.");
        if (segments.length != 3) {
            throw new UnauthorizedRequestException("Malformed Bearer token.");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(segments[1]);
            JsonNode payload = objectMapper.readTree(new String(payloadBytes, StandardCharsets.UTF_8));
            JsonNode courierIdNode = payload.get("courierId");

            if (courierIdNode == null) {
                throw new UnauthorizedRequestException("Bearer token does not contain a valid courierId claim.");
            }
            if (courierIdNode.isIntegralNumber()) {
                return courierIdNode.asLong();
            }
            if (courierIdNode.isTextual()) {
                return Long.parseLong(courierIdNode.asText());
            }
        } catch (UnauthorizedRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnauthorizedRequestException("Malformed Bearer token.");
        }

        throw new UnauthorizedRequestException("Bearer token does not contain a valid courierId claim.");
    }

    public String createExampleToken(Long courierId) {
        String header = base64UrlEncode(JWT_HEADER_JSON);
        String payload = base64UrlEncode("{\"courierId\":" + courierId + "}");
        return header + "." + payload + ".signature";
    }

    public String createAuthorizationHeader(Long courierId) {
        return BEARER_PREFIX + createExampleToken(courierId);
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
