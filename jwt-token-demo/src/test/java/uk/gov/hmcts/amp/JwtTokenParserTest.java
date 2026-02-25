package uk.gov.hmcts.amp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class JwtTokenParserTest {

    @InjectMocks
    JwtTokenParser jwtTokenParser;

    String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6InNNMV95QXhWOEdWNHlOLUI2ajJ4em1pazVBbyJ9" +
            ".eyJhdWQiOiJjOTg5YzQwMC05NjEwLTQyMTUtYWJiMC0zYmY4MDg1NTUyODAiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubG" +
            "luZS5jb20vZTI5OTVkMTEtOTk0Ny00ZTc4LTlkZTYtZDQ0ZTA2MDM1MThlL3YyLjAiLCJpYXQiOjE3NzIwMTkzMDYsIm5iZiI6MTc3MjAxOTMw" +
            "NiwiZXhwIjoxNzcyMDQ4NDA2LCJhaW8iOiJBU1FBMi84YkFBQUFTRXpZQXFYanBoLzl4Tit4aWw2ZDZNQWhUZVQyaktnMitHQnltK3IwVUw0PS" +
            "IsImF6cCI6IjEyMGQ2ZDRhLWQ0N2ItNGE0MC1hNzgyLTAwNjVmNDFkZTA1MCIsImF6cGFjciI6IjEiLCJvaWQiOiIxNzRhNjUzOC05MTk2LTRi" +
            "M2UtYjNhZi1jODZmOWU3MGViNGUiLCJyaCI6IjEuQVRFQUVWMlo0a2VaZUU2ZDV0Uk9CZ05SamdERWlja1FsaFZDcTdBNy1BaFZVb0FBQUFBeE" +
            "FBLiIsInN1YiI6IjE3NGE2NTM4LTkxOTYtNGIzZS1iM2FmLWM4NmY5ZTcwZWI0ZSIsInRpZCI6ImUyOTk1ZDExLTk5NDctNGU3OC05ZGU2LWQ0" +
            "NGUwNjAzNTE4ZSIsInV0aSI6Ind3UGMtOWd2SDBPMTR6WE5MVUVHQUEiLCJ2ZXIiOiIyLjAiLCJ4bXNfZnRkIjoiX3lSMUJaTnBaOUFKSjM5OH" +
            "YwYkh6S0dCaTdXZVV4OFhRUWs2djFMQW16Y0JaWFZ5YjNCbGQyVnpkQzFrYzIxeiJ9.he-zPUa3b3G7fia8eWHVFDvoVs4-YWOrjYQ3u3CbHDF" +
            "AdW8Oidz1fYH1EU4LckGfOLm8dqeTOt1rj0GBvDA91z0ZmPT4Fn-_VxWKE2NGsi8vG73EUF-rN2SgIPt-xVO27eO2LtI7l-kyq9zyAHMw_x7iy" +
            "uk3T7Hu2bMyNAOZ_4JshhtKOif8B1MVkK7Yd1A2WnAtZzfU4gTmzCJLbFtrPet1aO-V4bSLZC-5ec9RQVu1FyHGWFu4ygqlMehLPI0U_tJuk-O" +
            "syKWZb04m03e2ZWFQz8T-Pts2YWcwMSJdibcH-L4dpGhXjbEMVWajv5_jMnjC8FbYtRJG047NNSMH0g";

    @Test
    void jwt_token_with_azp_is_extracted() {
        JsonMapper jsonMapper = new JsonMapper();
        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String headerJson = new String(decoder.decode(chunks[0]));
        String payloadJson = new String(decoder.decode(chunks[1]));

        UUID clientId = jsonMapper.getUUIDAtPath(payloadJson, "/azp");
        assertThat(clientId).isEqualTo(UUID.fromString("120d6d4a-d47b-4a40-a782-0065f41de050"));
    }
}