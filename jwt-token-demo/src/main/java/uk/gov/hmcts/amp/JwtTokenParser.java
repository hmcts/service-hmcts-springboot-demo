package uk.gov.hmcts.amp;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class JwtTokenParser {


    public static String extractClientIdFromToken(final HttpServletRequest request, final ObjectMapper objectMapper) {
        // final String token = getBearerToken(request);
        String authToken = request.getHeader("Authorization");

        // getPayloadSegment ??
//        if (StringUtils.hasText(token)) {
//            final String payload = jwtPayload(token);
//            if (StringUtils.hasText(payload)) {
//                return getClientIdFromPayload(payload, objectMapper);
//            } else {
//                LOG.warn("Invalid or malformed JWT: could not decode payload");
//            }
//        }
        return null;
    }

    // Seems that this parsing the jwt ... hope we can use something else to decode it
//    private static String getPayloadSegment(final String token) {
//        String segment = null;
//        if (StringUtils.hasText(token)) {
//            final int firstIndex = token.indexOf('.');
//            if (firstIndex > 0) {
//                final int secondIndex = token.indexOf('.', firstIndex + 1);
//                final int endIndex = (secondIndex > firstIndex) ? secondIndex : token.length();
//                if (endIndex > firstIndex + 1) {
//                    final String b64 = token.substring(firstIndex + 1, endIndex);
//                    segment = padBase64Url(b64);
//                }
//            }
//        }
//        return segment;
//    }

    // why is it called padBase64Url ? is it related to url ? is it related to base64 ?
    // feels like it
    // actually, do we even need this? surely if we given a base64 string it would have the padding ??
    public String padBase64Url(final String b64) {
        final int pad = (4 - b64.length() % 4) % 4;
        return (pad == 0) ? b64 : b64 + "====".substring(0, pad);
    }

    public String decodeBase64Url(final String b64) {
        return new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8);
    }

}
