package fi.poltsi.vempain.admin;

import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.FormComponent;
import fi.poltsi.vempain.admin.repository.AclRepository;
import fi.poltsi.vempain.admin.repository.ComponentRepository;
import fi.poltsi.vempain.admin.repository.FormRepository;
import fi.poltsi.vempain.admin.repository.LayoutRepository;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.admin.repository.UnitRepository;
import fi.poltsi.vempain.admin.repository.UserRepository;
import fi.poltsi.vempain.admin.repository.file.FileCommonPageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileImagePageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.service.AclService;
import fi.poltsi.vempain.admin.service.ComponentService;
import fi.poltsi.vempain.admin.service.FormComponentService;
import fi.poltsi.vempain.admin.service.FormService;
import fi.poltsi.vempain.admin.service.LayoutService;
import fi.poltsi.vempain.admin.service.PageService;
import fi.poltsi.vempain.admin.service.PublishService;
import fi.poltsi.vempain.admin.service.UnitService;
import fi.poltsi.vempain.admin.service.UserService;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.tools.TestITCTools;
import fi.poltsi.vempain.admin.tools.TestUserAccountTools;
import fi.poltsi.vempain.site.repository.SitePageRepository;
import fi.poltsi.vempain.tools.JschClient;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.util.stream.StreamSupport;

import static fi.poltsi.vempain.tools.LocalFileTools.createAndVerifyDirectory;
import static fi.poltsi.vempain.tools.LocalFileTools.removeDirectory;

@Slf4j
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MockitoExtension.class)
public abstract class AbstractITCTest {
	@Autowired
	protected TestITCTools                 testITCTools;
	@Autowired
	protected TestUserAccountTools         testUserAccountTools;
	@Autowired
	private EntityManager				  entityManager;
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
	protected LayoutRepository             layoutRepository;
	@Autowired
	protected FileCommonPageableRepository fileCommonPageableRepository;
	@Autowired
	protected FileImagePageableRepository  fileImagePageableRepository;
	@Autowired
	protected FileThumbPageableRepository  fileThumbPageableRepository;
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
		// Check the state of the database
		// Pages
		var pages = pageRepository.findAll();

		if (hasItems(pages)) {
			log.warn("Removing orphaned Pages: {}", pages);
			pageRepository.deleteAll();
		}
		// Forms
		var forms = formRepository.findAll();
		if (hasItems(forms)) {
			for (Form form : forms) {
				var formComponents = formComponentService.findFormComponentByFormId(form.getId());

				if (hasItems(formComponents)) {
					log.warn("Removing orphaned formComponents: {}", formComponents);

					for (FormComponent formComponent : formComponents) {
						formComponentService.deleteFormComponent(formComponent.getFormId(), formComponent.getComponentId(), formComponent.getSortOrder());
					}
					pageRepository.deleteAll();
				}

				formRepository.deleteById(form.getId());
			}
		}

		// Components
		var components = componentRepository.findAll();

		if (hasItems(components)) {
			log.warn("Removing orphaned components: {}", components);
			componentRepository.deleteAll();
		}

		// Layouts
		var layouts = layoutRepository.findAll();

		if (hasItems(layouts)) {
			log.warn("Removing orphaned layouts: {}", layouts);
			layoutRepository.deleteAll();
		}

		// Galleries
		var galleries = galleryRepository.findAll();

		if (hasItems(galleries)) {
			log.warn("Removing orphaned layouts: {}", galleries);
			galleryRepository.deleteAll();
		}

		// FileCommon
		var fileCommons = fileCommonPageableRepository.findAll();

		if (hasItems(fileCommons)) {
			log.warn("Removing orphaned file common entries: {}", fileCommons);
			fileCommonPageableRepository.deleteAll();
		}

		// FileCommon
		var fileImages = fileImagePageableRepository.findAll();

		if (hasItems(fileImages)) {
			log.warn("Removing orphaned file common entries: {}", fileImages);
			fileImagePageableRepository.deleteAll();
		}

		// FileThumbs
		var fileThumbs = fileThumbPageableRepository.findAll();

		if (hasItems(fileThumbs)) {
			log.warn("Removing orphaned file common entries: {}", fileThumbs);
			fileThumbPageableRepository.deleteAll();
		}

		recreateUserUnitAcl();

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

	protected void recreateUserUnitAcl() {
		// ACL
		var acls = aclRepository.findAll();

		// Remove all ACLs and then generate the default ones
		if (hasItems(acls)) {
			log.warn("Removing orphaned ACLs: {}", acls);
			aclRepository.deleteAll();
		}

		entityManager.joinTransaction();

		entityManager.createNativeQuery("INSERT INTO acl (acl_id, read_privilege, create_privilege, modify_privilege, delete_privilege, unit_id, " +
											 "user_id) " +
										"VALUES (1, true, true, true, true, NULL, 1)," +
										" (2, true, true, true, true, NULL, 1)," +
										" (3, true, true, true, true, NULL, 1)," +
										" (4, true, true, true, true, NULL, 1)").executeUpdate();

		// Units
		var units = unitRepository.findAll();

		if (hasItems(units)) {
			log.warn("Removing orphaned units: {}", units);
			unitRepository.deleteAll();
		}

		entityManager.createNativeQuery("INSERT INTO unit (id, acl_id, created, creator, locked, modified, modifier, description, name)" +
										" OVERRIDING SYSTEM VALUE " +
										"VALUES (1, 2, NOW(), 1, false, null, null, 'Admin group', 'Admin')," +
										" (2, 3, NOW(), 1, false, null, null, 'Poweruser group', 'Poweruser')," +
										" (3, 4, NOW(), 1, false, null, null, 'Editor group', 'Editor')").executeUpdate();

		// Users
		var users = userRepository.findAll();

		if (hasItems(users)) {
			log.warn("Removing orphaned users: {}", users);
			userRepository.deleteAll();
		}

		entityManager.createNativeQuery("INSERT INTO user_account (id, acl_id, birthday, created, creator, locked, email, login_name, name, nick, password, priv_type, public_account, street)" +
										" OVERRIDING SYSTEM VALUE " +
										"VALUES (1, 1, '1900-01-01 00:00:00', NOW(), 1, false, 'admin@nohost.nodomain', 'admin', 'Vempain Administrator', 'Admin', 'Disabled', 'PRIVATE', false, '')").executeUpdate();

		entityManager.flush();
	}

	private static <T> boolean hasItems(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false)
							.findAny()
							.isPresent();
	}
}
