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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.util.stream.StreamSupport;

import static fi.poltsi.vempain.tools.LocalFileTools.createAndVerifyDirectory;
import static fi.poltsi.vempain.tools.LocalFileTools.removeDirectory;

@Slf4j
@SpringBootTest(properties = {"vempain.admin.file.converted-directory=/var/tmp/vempain-converted",
							  "vempain.admin.file.image-format=jpeg",
							  "vempain.site.www-root=/var/tmp/vempain-www",
							  "vempain.site.thumb-directory=.thumb",
							  "vempain.site.image-size=800",
							  "vempain.admin.file.thumbnail-size=250"})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MockitoExtension.class)
public abstract class AbstractITCTest {
	@Autowired
	protected TestITCTools                 testITCTools;
	@Autowired
	protected TestUserAccountTools         testUserAccountTools;
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

		// FileCommon
		var fileThumbs = fileThumbPageableRepository.findAll();

		if (hasItems(fileThumbs)) {
			log.warn("Removing orphaned file common entries: {}", fileThumbs);
			fileThumbPageableRepository.deleteAll();
		}

		// ACL
		var acls = aclRepository.findAll();

		if (hasItems(acls)) {
			log.warn("Removing orphaned ACLs: {}", acls);
			aclRepository.deleteAll();
		}

		// Units
		var units = unitRepository.findAll();

		if (hasItems(units)) {
			log.warn("Removing orphaned units: {}", units);
			unitRepository.deleteAll();
		}

		// Users
		var users = userRepository.findAll();

		if (hasItems(users)) {
			log.warn("Removing orphaned users: {}", users);
			userRepository.deleteAll();
		}

		// Files and paths
		log.info("=============================================================");
		log.info("Cleaning up directories");
		removeDirectory("/var/tmp/vempain-converted");
		createAndVerifyDirectory(Path.of("/var/tmp/vempain-converted"));
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
