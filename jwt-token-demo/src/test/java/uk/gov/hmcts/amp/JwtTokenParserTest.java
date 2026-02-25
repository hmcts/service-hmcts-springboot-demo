package uk.gov.hmcts.amp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenParserTest {

    @InjectMocks
    JwtTokenParser jwtTokenParser;

    @Test
    void pad_should_pad_to_multiple_of_4() {
        assertThrows(Exception.class, ()->jwtTokenParser.padBase64Url(null));
        assertThat(jwtTokenParser.padBase64Url("")).isEqualTo("");
        assertThat(jwtTokenParser.padBase64Url("xx")).isEqualTo("xx==");
        assertThat(jwtTokenParser.padBase64Url("yyyyy")).isEqualTo("yyyyy===");
        assertThat(jwtTokenParser.padBase64Url("zzzz")).isEqualTo("zzzz");
    }

    @Test
    void base64_should_do_its_stuff(){
        assertThrows(Exception.class, ()->jwtTokenParser.decodeBase64Url(null));
        assertThrows(Exception.class, ()->jwtTokenParser.decodeBase64Url("not base 64"));
        assertThat(jwtTokenParser.decodeBase64Url("SGVsbG8gd29ybGQ=")).isEqualTo("Hello world");
    }

}