package com.justinus.api.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for controller integration tests. Shares ONE real Postgres
 * container across every test class in the run, started once here and
 * never stopped between classes.
 *
 * Deliberately NOT using @Testcontainers/@Container: those JUnit
 * lifecycle annotations stop the container after each test class and
 * start a fresh one (new port) for the next. Spring's test framework
 * then reuses its cached ApplicationContext across classes whose
 * config looks identical -- which still points at the old, now-dead
 * container's port. Every request in the second class fails with
 * "Connection refused" even though a new container is running. A
 * manually-started singleton avoids the mismatch entirely.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    static {
        POSTGRES.start();
    }
}
