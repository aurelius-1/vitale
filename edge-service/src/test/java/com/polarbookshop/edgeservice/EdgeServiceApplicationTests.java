package com.polarbookshop.edgeservice;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EdgeServiceApplicationTests {

    private static final int REDIS_PORT = 6379;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(REDIS_PORT);

    @Autowired
    WebTestClient webTestClient;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> redis.getHost());
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(REDIS_PORT));
    }

    @Test
    void contextLoads() {
    }

    @Test
    void whenUnauthenticatedGetBooksThenNotUnauthorized() {
        // GET /books e public — circuit breaker returneaza 200 (fallback) chiar fara catalog service
        webTestClient
                .get().uri("/books")
                .exchange()
                .expectStatus().value(status -> Assertions.assertThat(status).isNotEqualTo(401));
    }

    @Test
    void whenUnauthenticatedPostBooksThenUnauthorized() {
        // POST /books necesita autentificare
        webTestClient
                .mutateWith(csrf())
                .post().uri("/books")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenAuthenticatedPostBooksThenNotUnauthorized() {
        // user autentificat trece de security (poate fi 503 daca catalog service nu e disponibil, dar nu 401)
        webTestClient
                .mutateWith(mockUser("user").roles("USER"))
                .mutateWith(csrf())
                .post().uri("/books")
                .exchange()
                .expectStatus().value(status -> Assertions.assertThat(status).isNotEqualTo(401));
    }
}
