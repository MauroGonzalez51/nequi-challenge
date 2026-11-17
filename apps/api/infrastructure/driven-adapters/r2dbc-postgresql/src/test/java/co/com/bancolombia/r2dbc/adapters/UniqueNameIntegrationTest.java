package co.com.bancolombia.r2dbc.adapters;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivecommons.utils.ObjectMapper;
import org.reactivecommons.utils.ObjectMapperImp;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import co.com.bancolombia.model.exception.DataConflictException;
import co.com.bancolombia.model.franchise.Franchise;
import co.com.bancolombia.r2dbc.helper.TestResilience;
import co.com.bancolombia.r2dbc.repositories.FranchiseEntityRepository;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import reactor.test.StepVerifier;

/**
 * Runs the real adapter against a real PostgreSQL (Docker) with the real
 * schema.sql, so the unique
 * indexes and the violation -> DataConflictException mapping are tested, not
 * mocked.
 */
@Testcontainers
class UniqueNameIntegrationTest {

    // schema.sql lives in app-service; Gradle runs tests from this module's
    // directory.
    private static final Path SCHEMA = Path
            .of("../../../applications/app-service/src/main/resources/schema.sql");

    // "trust" skips the password handshake: SCRAM auth generates a nonce with
    // SecureRandom, which reads
    // /dev/urandom on the Netty thread and BlockHound reports it. Safe here: a
    // throwaway test container.
    @SuppressWarnings("resource")
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust");

    private static FranchiseRepositoryAdapter adapter;

    @BeforeAll
    static void createSchemaAndAdapter() throws IOException {
        // BlockHound is active in this project: resolve the host to an IP here, outside
        // any reactive
        // thread, because Netty resolving "localhost" reads /etc/hosts and counts as a
        // blocking call.
        String hostAddress = InetAddress.getByName(POSTGRES.getHost()).getHostAddress();

        PostgresqlConnectionFactory connectionFactory = new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                        .host(hostAddress)
                        .port(POSTGRES.getMappedPort(5432))
                        .database(POSTGRES.getDatabaseName())
                        .username(POSTGRES.getUsername())
                        .password(POSTGRES.getPassword())
                        .build());

        // The script is read here (blocking is fine on the main thread) and handed over
        // as bytes.
        ByteArrayResource script = new ByteArrayResource(Files.readAllBytes(SCHEMA));
        StepVerifier.create(new ResourceDatabasePopulator(script).populate(connectionFactory))
                .verifyComplete();

        FranchiseEntityRepository repository = new R2dbcRepositoryFactory(
                new R2dbcEntityTemplate(connectionFactory)).getRepository(FranchiseEntityRepository.class);
        ObjectMapper mapper = new ObjectMapperImp();

        adapter = new FranchiseRepositoryAdapter(repository, mapper, TestResilience.create());
    }

    @Test
    void shouldSaveAndReadBackAFranchise() {
        StepVerifier.create(adapter.save(Franchise.builder().name("Persisted").build())
                .flatMap(saved -> adapter.findById(saved.getId())))
                .expectNextMatches(found -> found.getId() != null && found.getName().equals("Persisted"))
                .verifyComplete();
    }

    @Test
    void shouldRejectADuplicateNameIgnoringCaseAndSurroundingSpaces() {
        StepVerifier.create(adapter.save(Franchise.builder().name("Pizza").build())
                .then(adapter.save(Franchise.builder().name("  pIZZA ").build())))
                .expectError(DataConflictException.class)
                .verify();
    }
}
