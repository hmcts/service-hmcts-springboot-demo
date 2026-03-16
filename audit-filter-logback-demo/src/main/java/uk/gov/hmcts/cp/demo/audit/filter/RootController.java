package uk.gov.hmcts.cp.demo.audit.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
public class RootController {

    private static final Logger log = LoggerFactory.getLogger(RootController.class);

    private static final List<String> QUOTES = List.of(
        "The only way to do great work is to love what you do.",
        "Innovation distinguishes between a leader and a follower.",
        "Stay hungry, stay foolish.",
        "The future belongs to those who believe in the beauty of their dreams.",
        "It does not matter how slowly you go as long as you do not stop."
    );

    private final Random random = new Random();

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        LocalDateTime now = LocalDateTime.now();
        String dateTime = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String date = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String quote = QUOTES.get(random.nextInt(QUOTES.size()));

        log.info("Root request - Date: {}, Time: {}, Quote: \"{}\"", date, time, quote);

        return ResponseEntity.ok(Map.of(
            "date", date,
            "time", time,
            "dateTime", dateTime,
            "quote", quote
        ));
    }
}
