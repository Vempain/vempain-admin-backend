package fi.poltsi.vempain;

import fi.poltsi.vempain.admin.service.AclService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ContainerTestITC {
    @Container
    public static PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:latest")
        .withDatabaseName("vempain_admin")
        .withUsername("test")
        .withPassword("test");

	@Autowired
	@Qualifier("adminFlyway")
	private Flyway adminFlyway;

	@Autowired
	protected AclService aclService;

	@BeforeEach
	public void resetDatabase() {
		adminFlyway.clean();
		adminFlyway.migrate();
	}

	@Test
	void testCase() {
		var acls = aclService.findAll();
		assertNotNull(acls);
	}
}
