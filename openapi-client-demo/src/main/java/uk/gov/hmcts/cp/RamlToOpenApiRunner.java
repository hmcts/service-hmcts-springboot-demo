package uk.gov.hmcts.cp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class RamlToOpenApiRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RamlToOpenApiRunner.class);
    private static final Path RAML = Paths.get("src/main/resources/raml/material-query-api.raml");
    private static final Path OUT = Paths.get("src/main/resources/openapi/material-query-api.yml");

    @Override
    public void run(String... args) throws Exception {
        if (!Files.exists(RAML)) {
            log.info("No RAML at {}, skipping conversion.", RAML);
            return;
        }
        log.info("Converting RAML {} -> OpenAPI (in-memory)", RAML);

        ProcessBuilder pb = new ProcessBuilder(
                "npx", "-y", "oas-raml-converter",
                "--from", "raml",
                "--to", "openapi",
                RAML.toAbsolutePath().toString()
        );
        Process p = pb.start();

        try (InputStream is = p.getInputStream()) {
            String yaml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            int exit = p.waitFor();

            if (exit != 0 || yaml.isBlank()) {
                // capture stderr for diagnostics if available
                String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("oas-raml-converter failed (exit=" + exit + "): " + err.trim());
            }

            // validate in-memory
            SwaggerParseResult res = new OpenAPIV3Parser().readContents(yaml, null, null);
            if (res.getOpenAPI() == null || (res.getMessages() != null && !res.getMessages().isEmpty())) {
                String msgs = (res.getMessages() == null) ? "no details" : String.join("; ", res.getMessages());
                throw new IllegalStateException("OpenAPI validation failed: " + msgs);
            }

            // write only after successful validation
            Files.createDirectories(OUT.getParent());
            Files.writeString(OUT, yaml, StandardCharsets.UTF_8);
            log.info("Converted and validated OpenAPI written to {}", OUT);
        }
    }
}