package fi.poltsi.vempain.admin;

import fi.poltsi.vempain.admin.repository.ComponentRepository;
import fi.poltsi.vempain.admin.repository.FormRepository;
import fi.poltsi.vempain.admin.repository.LayoutRepository;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.ComponentService;
import fi.poltsi.vempain.admin.service.FormComponentService;
import fi.poltsi.vempain.admin.service.FormService;
import fi.poltsi.vempain.admin.service.LayoutService;
import fi.poltsi.vempain.admin.service.PageService;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.admin.service.UnitService;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.tools.TestITCTools;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserRepository;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.site.repository.SitePageRepository;
import fi.poltsi.vempain.tools.JschClient;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.util.stream.StreamSupport;

import static fi.poltsi.vempain.tools.LocalFileTools.createAndVerifyDirectory;
import static fi.poltsi.vempain.tools.LocalFileTools.removeDirectory;

@Slf4j
@Transactional
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MockitoExtension.class)
public abstract class AbstractITCTest {

	@Container
	public static PostgreSQLContainer<?> vempainAdminContainer = new PostgreSQLContainer<>("postgres:17")
			.withDatabaseName("vempain_admin")
			.withUsername("test")
			.withPassword("test");

	@Container
	public static PostgreSQLContainer<?> vempainSiteContainer = new PostgreSQLContainer<>("postgres:17")
			.withDatabaseName("vempain_site")
			.withUsername("test")
			.withPassword("test");

	@Autowired
	@Qualifier("adminFlyway")
	private Flyway adminFlyway;

	@Autowired
	@Qualifier("siteFlyway")
	private Flyway siteFlyway;

	@Autowired
	protected TestITCTools                 testITCTools;
	@Autowired
	private   EntityManager                entityManager;
	// Services
	@Autowired
	protected AclService                   aclService;
	@Autowired
	protected FormService                  formService;
	@Autowired
	protected ComponentService             componentService;
	@Autowired
	protected LayoutService                layoutService;
	@Autowired
	protected FileService                  fileService;
	@Autowired
	protected PageService                  pageService;
	@Autowired
	protected FormComponentService         formComponentService;
	@Autowired
	protected UnitService                  unitService;
	@Autowired
	protected UserService                  userService;
	@Autowired
	protected PublishService               publishService;
	// Repositories
	@Autowired
	protected AclRepository                aclRepository;
	@Autowired
	protected FormRepository               formRepository;
	@Autowired
	protected ComponentRepository          componentRepository;
	@Autowired
	protected LayoutRepository            layoutRepository;
	@Autowired
	protected SiteFileRepository          siteFileRepository;
	@Autowired
	protected FileThumbPageableRepository fileThumbPageableRepository;
	@Autowired
	protected PageRepository               pageRepository;
	@Autowired
	protected UnitRepository               unitRepository;
	@Autowired
	protected UserRepository               userRepository;
	@Autowired
	protected GalleryRepository            galleryRepository;
	@Autowired
	protected SitePageRepository           sitePageRepository;
	@Autowired
	protected JschClient                   jschClient;

	@Value("${vempain.site.www-root}")
	private String siteWwwRoot;
	@Value("${vempain.site.ssh.home-dir}")
	private String siteSshHomeDir;

	@Transactional
	@BeforeEach
	void setUp() {
		adminFlyway.clean();
		adminFlyway.migrate();

		siteFlyway.clean();
		siteFlyway.migrate();

		// Files and paths
		log.info("=============================================================");
		log.info("Cleaning up directories");
		removeDirectory("/var/tmp/vempain-converted");
		createAndVerifyDirectory(Path.of("/var/tmp/vempain-converted"));
		log.info("Site WWW-root is: {}", siteWwwRoot);
		log.info("Site SSH home directory is: {}", siteSshHomeDir);
		removeDirectory("/var/tmp/vempain-www");
		createAndVerifyDirectory(Path.of("/var/tmp/vempain-www"));
		log.info("=============================================================");
	}

	private static <T> boolean hasItems(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false)
							.findAny()
							.isPresent();
	}
}
