package com.justinus.api.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.neo4j.Neo4jContainer;

/**
 * Base class for controller integration tests. Shares ONE real Neo4j
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
    static final Neo4jContainer NEO4J = new Neo4jContainer("neo4j:5-community");

    static {
        NEO4J.start();
    }
}
