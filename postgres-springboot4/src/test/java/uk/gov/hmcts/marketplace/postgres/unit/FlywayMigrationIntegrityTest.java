package uk.gov.hmcts.marketplace.postgres.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Guards against modification of existing Flyway migration scripts, and ensures
 * every migration file has a registered checksum.
 * <p>
 * Flyway uses checksums to detect tampering — any change to an applied migration
 * causes startup failures in all environments. If this test fails because:
 * - a checksum mismatches: restore the original file and create a new migration instead
 * - a file is unregistered: add the SHA-256 shown in the failure to EXPECTED_CHECKSUMS below
 */
class FlywayMigrationIntegrityTest {

    private static final String MIGRATION_DIR = "/db/migration";

    private static final Map<String, String> EXPECTED_CHECKSUMS = Map.ofEntries(
            Map.entry("V1.001__initial_schema.sql", "970b13ac9b04cfefd50b4bc13897df2c75ec18db89200506f80f9ddda379a456")
    );

    @Test
    void all_migration_files_must_have_a_registered_checksum()
            throws URISyntaxException, IOException, NoSuchAlgorithmException {
        List<String> migrationFiles = listMigrationFiles();
        assertThat(migrationFiles).isNotEmpty();

        List<String> unregistered = migrationFiles.stream()
                .filter(f -> !EXPECTED_CHECKSUMS.containsKey(f))
                .toList();

        if (!unregistered.isEmpty()) {
            StringBuilder hint = new StringBuilder(
                    "New migration(s) found without a registered checksum. "
                            + "Add the following to EXPECTED_CHECKSUMS in FlywayMigrationIntegrityTest:\n");
            for (String filename : unregistered) {
                String checksum = sha256(MIGRATION_DIR + "/" + filename);
                hint.append(String.format("  \"%s\", \"%s\"%n", filename, checksum));
            }
            fail(hint.toString());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("registeredMigrations")
    void migration_script_must_not_be_modified(String filename, String expectedSha256)
            throws IOException, NoSuchAlgorithmException {
        String actual = sha256(MIGRATION_DIR + "/" + filename);
        assertThat(actual)
                .as("Migration '%s' has been modified. "
                        + "Editing applied Flyway migrations breaks all environments. "
                        + "Create a new migration script instead.", filename)
                .isEqualTo(expectedSha256);
    }

    static Stream<Arguments> registeredMigrations() {
        return EXPECTED_CHECKSUMS.entrySet().stream()
                .map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    private List<String> listMigrationFiles() throws URISyntaxException {
        URL dirUrl = getClass().getResource(MIGRATION_DIR);
        assertThat(dirUrl).as("Migration directory not found on classpath: %s", MIGRATION_DIR).isNotNull();
        File dir = new File(dirUrl.toURI());
        String[] files = dir.list((d, name) -> name.endsWith(".sql"));
        assertThat(files).as("No .sql files found in %s", MIGRATION_DIR).isNotNull();
        return Arrays.asList(files);
    }

    private String sha256(String classpathResource) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(classpathResource)) {
            assertThat(in)
                    .as("Migration file not found on classpath: %s", classpathResource)
                    .isNotNull();
            try (DigestInputStream dis = new DigestInputStream(in, digest)) {
                dis.transferTo(OutputStream.nullOutputStream());
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
