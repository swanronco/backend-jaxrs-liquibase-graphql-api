package com.example.user;

import com.example.model.User;
import com.example.util.JpaUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void setup() {
        JpaUtil.init(Map.of(
            "jakarta.persistence.jdbc.url", postgres.getJdbcUrl(),
            "jakarta.persistence.jdbc.user", postgres.getUsername(),
            "jakarta.persistence.jdbc.password", postgres.getPassword(),
            "hibernate.hbm2ddl.auto", "create"
        ));
    }

    @Test
    void testSaveAndFind() {
        UserRepository repository = new UserRepository();
        User user = new User(null, "Swan", "Ronco", "swan@example.com", "swanronco", "hashedpassword");
        repository.save(user);

        User found = repository.findById(user.getId());
        assertNotNull(found, "L'utilisateur devrait être trouvé en base");
    }
}
