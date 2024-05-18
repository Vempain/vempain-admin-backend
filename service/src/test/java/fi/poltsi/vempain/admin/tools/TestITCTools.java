package fi.poltsi.vempain.admin.tools;

import fi.poltsi.vempain.admin.api.response.ComponentResponse;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import fi.poltsi.vempain.admin.api.response.PrivacyType;
import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.FormComponent;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.entity.Unit;
import fi.poltsi.vempain.admin.entity.UserAccount;
import fi.poltsi.vempain.admin.entity.file.FileCommon;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.exception.VempainLayoutException;
import fi.poltsi.vempain.admin.repository.AclRepository;
import fi.poltsi.vempain.admin.repository.ComponentRepository;
import fi.poltsi.vempain.admin.repository.FormRepository;
import fi.poltsi.vempain.admin.repository.LayoutRepository;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.admin.repository.UserRepository;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.service.AclService;
import fi.poltsi.vempain.admin.service.ComponentService;
import fi.poltsi.vempain.admin.service.FormComponentService;
import fi.poltsi.vempain.admin.service.FormService;
import fi.poltsi.vempain.admin.service.LayoutService;
import fi.poltsi.vempain.admin.service.UnitService;
import fi.poltsi.vempain.admin.service.UserService;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.tools.JsonTools;
import fi.poltsi.vempain.tools.MetadataTools;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static fi.poltsi.vempain.admin.api.Constants.ADMIN_ID;
import static fi.poltsi.vempain.admin.api.FileClassEnum.getFileClassByMimetype;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@Getter
@org.springframework.stereotype.Component
public class TestITCTools {
	private final AclService           aclService;
	private final FormService          formService;
	private final ComponentService     componentService;
	private final LayoutService        layoutService;
	private final FileService          fileService;
	private final FormComponentService formComponentService;
	private final AclRepository        aclRepository;
	private final FormRepository       formRepository;
	private final ComponentRepository  componentRepository;
	private final LayoutRepository     layoutRepository;
	private final UserService          userService;
	private final UnitService          unitService;
	private final UserRepository       userRepository;
	private final PageRepository       pageRepository;
	private final GalleryRepository    galleryRepository;
	@Value("${vempain.admin.file.converted-directory}")
	private       String               convertedDirectory;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	public TestITCTools(AclService aclService, FormService formService, ComponentService componentService, LayoutService layoutService,
						FileService fileService, FormComponentService formComponentService, AclRepository aclRepository,
						FormRepository formRepository, ComponentRepository componentRepository, LayoutRepository layoutRepository,
						UserService userService, UnitService unitService, UserRepository userRepository, PageRepository pageRepository,
						GalleryRepository galleryRepository) {
		this.aclService           = aclService;
		this.formService          = formService;
		this.componentService     = componentService;
		this.layoutService        = layoutService;
		this.fileService          = fileService;
		this.formComponentService = formComponentService;
		this.aclRepository        = aclRepository;
		this.formRepository       = formRepository;
		this.componentRepository  = componentRepository;
		this.layoutRepository     = layoutRepository;
		this.userService          = userService;
		this.unitService          = unitService;
		this.userRepository       = userRepository;
		this.pageRepository       = pageRepository;
		this.galleryRepository    = galleryRepository;
	}

	/////////////////// Acls start
	public Long generateAcl(Long userId, Long unitId, boolean read, boolean modify, boolean create, boolean delete) {
		long aclId = aclService.getNextAclId();
		return generateAclWithId(aclId, userId, unitId, read, modify, create, delete);
	}

	public Long generateAclWithId(Long aclId, Long userId, Long unitId, boolean read, boolean modify, boolean create, boolean delete) {
		Acl acl = Acl.builder()
					 .aclId(aclId)
					 .createPrivilege(create)
					 .deletePrivilege(delete)
					 .modifyPrivilege(modify)
					 .readPrivilege(read)
					 .unitId(unitId)
					 .userId(userId)
					 .build();
		try {
			var newAcl = aclService.save(acl);
			aclId = newAcl.getAclId();
			log.info("Generated new ACL ID: {}", aclId);
		} catch (VempainAclException e) {
			log.error("Failed to create Acl");
			fail("Unable to create Acl");
		}

		return aclId;
	}

	public List<Long> generateAcls(long counter) {
		var aclList = new ArrayList<Long>();

		for (long i = 0; i < counter; i++) {
			var userId  = generateUser();
			var nextAcl = aclService.getNextAclId();
			aclList.add(nextAcl);
			log.info("Creating acl with aclId: {}", nextAcl);
			generateAclWithId(nextAcl, userId, null, true, true, true, true);
			var unitId = generateUnit();
			generateAclWithId(nextAcl, null, unitId, true, true, true, true);
		}

		Iterable<Acl> acls = aclService.findAll();
		return aclList;
	}
	/////////////////// Acls end

	/////////////////// Forms start
	public Long generateForm() {
		var formId   = formRepository.getNextAvailableFormId();
		var layoutId = generateLayout();

		var userId = generateUser();
		var aclId  = generateAcl(userId, null, true, true, true, true);
		var form = Form.builder()
					   .formName("Test form " + formId)
					   .layoutId(layoutId)
					   .aclId(aclId)
					   .locked(false)
					   .creator(userId)
					   .created(Instant.now()
									   .minus(1, ChronoUnit.HOURS))
					   .modifier(null)
					   .modified(null)
					   .build();
		var result = formService.save(form);
		assertForm(result);
		return result.getId();
	}

	public List<Long> generateForms(long count) {
		var formIdList = new ArrayList<Long>();

		for (long i = 1; i <= count; i++) {
			formIdList.add(generateForm());
		}

		return formIdList;
	}

	public void assertForm(Form form) {
		assertNotNull(form);
		assertTrue(form.getCreator() > 0);
		assertNotNull(form.getCreated());
		assertNull(form.getModifier());
		assertTrue(form.getLayoutId() > 0);
		assertTrue(form.getAclId() > 0);
		assertNull(form.getModified());
		assertNotNull(form.getFormName());
		assertTrue(form.getFormName()
					   .contains("Test form "));
	}
	/////////////////// Forms end

	/////////////////// FormComponents start
	public FormComponent generateFormComponent(long formId, long componentId, long sortOrder) {
		var optionalComponent = componentRepository.findById(componentId);

		if (optionalComponent.isEmpty()) {
			log.error("Could not generate a form component as component with ID {} does not exist", componentId);
			return null;
		}

		formComponentService.addFormComponent(formId, componentId, sortOrder);
		return FormComponent.builder()
							.formId(formId)
							.componentId(componentId)
							.sortOrder(sortOrder)
							.build();
	}

	public Map<Long, List<Long>> generateFormComponent(long formId, long componentCount) throws VempainComponentException, VempainAbstractException {
		var  componentIds = generateComponents(componentCount);
		long sortOrder    = 0L;
		var  map          = new HashMap<Long, List<Long>>();

		for (Long componentId : componentIds) {
			generateFormComponent(formId, componentId, sortOrder);
			sortOrder++;
		}

		map.put(formId, componentIds);
		return map;
	}

	public List<Map<Long, List<Long>>> generateFormComponents(long formCount, long componentCount) throws VempainComponentException, VempainAbstractException,
																										  VempainEntityNotFoundException {
		var formIds           = generateForms(formCount);
		var formComponentList = new ArrayList<Map<Long, List<Long>>>();

		for (Long formId : formIds) {
			var formComponentMap = generateFormComponent(formId, componentCount);
			formComponentList.add(formComponentMap);
		}

		return formComponentList;
	}
	/////////////////// FormComponents end

	/////////////////// Components start
	public Component generateComponent(long index) throws VempainComponentException, VempainAbstractException {
		var userId = generateUser();
		var aclId  = generateAcl(userId, null, true, true, true, true);
		Component component = Component.builder()
									   .compName("Test component " + index + RandomStringUtils.randomAlphanumeric(8))
									   .compData("Test component data " + index)
									   .locked(false)
									   .aclId(aclId)
									   .creator(userId)
									   .created(Instant.now().minus(1, ChronoUnit.HOURS))
									   .modifier(null)
									   .modified(null)
									   .build();
		return componentService.save(component);
	}

	public List<Long> generateComponents(long counter) throws VempainComponentException, VempainAbstractException {
		var createdComponentIds = new ArrayList<Long>();

		for (long i = 1; i <= counter; i++) {
			var component = generateComponent(i);
			createdComponentIds.add(component.getId());
		}

		return createdComponentIds;
	}

	public void verifyComponentResponse(ComponentResponse response) {
		assertNotNull(response);

		assertNotNull(response.getAcls());
		assertFalse(response.getAcls()
							.isEmpty());
		assertNotNull(response.getCompData());
		assertNotNull(response.getCompName());
		assertNotNull(response.getCreator());
		assertNotNull(response.getCreated());

		if (response.getModifier() == null) {
			assertNull(response.getModified());
		} else {
			assertNotNull(response.getModified(), "Modified field should not be null when modifier is set: " + response.getId());
		}
	}

	public void addComponentsToForm(long formId, List<Long> componentIdList) {
		var optionalForm = formRepository.findById(formId);

		if (optionalForm.isEmpty()) {
			log.error("Failed to find a form with ID {}", formId);
		}

		Long sortOrder = 0L;

		for (Long componentId : componentIdList) {
			var optionalComponent = componentRepository.findById(componentId);

			if (optionalComponent.isEmpty()) {
				log.error("Failed to find a component with ID {}", componentId);
			}

			formComponentService.addFormComponent(formId, componentId, sortOrder);
			sortOrder++;
		}
	}
	/////////////////// Components end

	/////////////////// Layouts start
	public Long generateLayout() {
		var  userId = generateUser();
		long aclId  = generateAcl(userId, null, true, true, true, true);
		var layout = Layout.builder()
						   .layoutName("Test layout " + userId)
						   .structure("<!--comp_0--><!--comp_1--><!--page--><!--comp_2-->")
						   .locked(false)
						   .aclId(aclId)
						   .creator(userId)
						   .created(Instant.now()
										   .minus(1, ChronoUnit.HOURS))
						   .modifier(null)
						   .modified(null)
						   .build();
		try {
			var newLayout = layoutService.save(layout);
			log.info("Generated new layout with ID: {}", newLayout.getId());
			return newLayout.getId();
		} catch (VempainLayoutException | VempainAbstractException e) {
			fail("Unable to create a test layout: " + e);
		}

		return null;
	}

	public List<Long> generateLayouts(long count) {
		var layoutIds = new ArrayList<Long>();
		for (long i = 0; i < count; i++) {
			layoutIds.add(generateLayout());
		}

		return layoutIds;
	}

	public boolean compareLayouts(LayoutResponse layout1, LayoutResponse layout2) {
		if (layout1.getId() == layout2.getId()) {
			log.info("LayoutId matches");
			if (layout1.getLayoutName()
					   .equals(layout2.getLayoutName())) {
				log.info("Layout name matches");
				if (layout1.getAcls() == layout2.getAcls()) {
					log.info("AclId matches");
					if (layout1.getStructure()
							   .equals(layout2.getStructure())) {
						log.info("Structure matches");
						if (layout1.getCreator()
								   .equals(layout2.getCreator())) {
							log.info("Creator matches");
							if (layout1.getModifier()
									   .equals(layout2.getModifier())) {
								log.info("Modifier matches");
								if (ChronoUnit.SECONDS.between(layout1.getCreated(), layout2.getCreated()) < 2) {
									log.info("Created tolerance ok {} {}", layout1.getCreated(), layout2.getCreated());
									return ChronoUnit.SECONDS.between(layout1.getModified(), layout2.getModified()) < 2;
								}
							} else {
								log.info("Modifier differs {} -> {}", layout1.getModifier(), layout2.getModifier());
							}
						}
					} else {
						log.warn("Structure did not match:\n{}\n{}", layout1.getStructure(), layout2.getStructure());
					}
				}
			}
		}

		return false;
	}
	/////////////////// Layouts end

	/////////////////// Page start
	public Long generatePage() throws VempainAbstractException, VempainComponentException {
		var formId       = generateForm();
		var userId       = generateUser();
		var aclId        = generateAcl(userId, null, true, true, true, true);
		var componentIds = generateComponents(3L);
		log.info("Generated components: {}", componentIds);
		long order = 0L;

		for (Long componentId : componentIds) {
			generateFormComponent(formId, componentId, order);
			order++;
		}

		var page = Page.builder()
					   .aclId(aclId)
					   .formId(formId)
					   .header("Test page header " + userId)
					   .title("Test page title " + userId)
					   .path("/test/testuser_" + userId)
					   .body("Test page body")
					   .indexList(false)
					   .locked(false)
					   .secure(true)
					   .creator(userId)
					   .created(Instant.now())
					   .modifier(null)
					   .modified(null)
					   .build();
		var newPage = pageRepository.save(page);
		assertNotNull(newPage);

		return newPage.getId();
	}

	/////////////////// Page end
	/////////////////// Subject start
	public Subject generateSubject(long index) {
		var subject = Subject.builder()
							 .subjectName("Subject " + index)
							 .subjectNameDe("Subject DE " + index)
							 .subjectNameEn("Subject EN " + index)
							 .subjectNameFi("Subject FI " + index)
							 .subjectNameSe("Subject SE " + index)
							 .build();

		return fileService.saveSubject(subject);
	}

	public List<Long> generateSubjects(long counter) {
		var subjectIdList = new ArrayList<Long>();

		for (long i = 1; i <= counter; i++) {
			var subject = generateSubject(i);
			subjectIdList.add(subject.getId());
		}

		return subjectIdList;
	}

	/////////////////// Subject end
	/////////////////// FileCommon start
	public FileCommon generateFileCommon(long idx) {
		var aclId = generateAcl(1L, null, true, true, true, true);
		var fileCommon = FileCommon.builder()
								   .convertedFile("site/" + idx + ".jpg")
								   .convertedFilesize(100L + idx)
								   .convertedSha1sum("so-Sha1sum" + idx)
								   .siteFilename(idx + ".jpg")
								   .siteFilepath("site/")
								   .siteSha1sum("si-Sha1sum" + idx)
								   .metadata("")
								   .fileClassId(1L)
								   .comment("")
								   .aclId(aclId)
								   .creator(1L)
								   .created(Instant.now()
												   .minus(1, ChronoUnit.HOURS))
								   .modifier(1L)
								   .modified(Instant.now())
								   .build();
		return fileService.saveFileCommon(fileCommon);
	}

	public List<Long> generateFileCommons(long counter) {
		var fileCommonIdList = new ArrayList<Long>();

		for (long i = 1; i <= counter; i++) {
			var fileCommon = generateFileCommon(i);
			fileCommonIdList.add(fileCommon.getId());
		}

		return fileCommonIdList;
	}

	/////////////////// FileCommon end
	/////////////////// FileSubject start
	/////////////////// FileSubject end
	/////////////////// User start
	@Transactional
	public Long generateUser() {
		var testUserAccountTools = new TestUserAccountTools();
		var password             = testUserAccountTools.randomLongString();
		return generateUser(password);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Long generateUser(String password) {
		var testUserAccountTools = new TestUserAccountTools();
		// Note that here the user ID used to generate the ACL refers to the admin
		var aclId = generateAcl(ADMIN_ID, null, true, true, true, true);
		var user = UserAccount.builder()
							  .aclId(aclId)
							  .birthday(Instant.now().minus(20 * 365, ChronoUnit.DAYS))
							  .created(Instant.now().minus(1, ChronoUnit.HOURS))
							  .creator(ADMIN_ID)
							  .description("ITC generated user " + password)
							  .email("first." + password + "@test.tld")
							  .locked(false)
							  .loginName(password)
							  .modified(Instant.now())
							  .modifier(ADMIN_ID)
							  .name("Firstname " + password)
							  .nick(password)
							  .password(testUserAccountTools.encryptPassword(password))
							  .pob("1111")
							  .privacyType(PrivacyType.PRIVATE)
							  .isPublic(false)
							  .street("")
							  .units(null)
							  .build();

		var newUser = userService.save(user);
		entityManager.flush();

		// Once we have generated a user, we can generate the ACL for the object
		var userAclID = generateAcl(newUser.getId(), null, true, true, true, true);
		// Then update the aciId on the user
		newUser.setAclId(userAclID);
		// We can also update the creator and modifier to be correct
		newUser.setCreator(newUser.getId());
		newUser.setModifier(newUser.getId());
		var newestUser = userService.save(newUser);
		entityManager.flush();
		// We also need to create a unit for the user
		var unitAclID = generateAcl(newestUser.getId(), null, true, true, true, true);
		log.info("New unit ACL ID for the user: {}", unitAclID);
		var unit = Unit.builder()
					   .description("ITC generated unit for user " + password)
					   .name("Unit " + password)
					   .aclId(unitAclID)
					   .locked(false)
					   .created(Instant.now()
									   .minus(1, ChronoUnit.HOURS))
					   .creator(newUser.getId())
					   .build();

		unitService.save(unit);

		entityManager.flush();
		return newUser.getId();
	}

	public List<Long> generateUsers(Long count) {
		var idList = new ArrayList<Long>();

		for (long counter = 0L; counter < count; counter++) {
			idList.add(generateUser());
		}

		return idList;
	}

	/////////////////// User end
	/////////////////// Unit start
	public Long generateUnit() {
		var testUserAccountTools = new TestUserAccountTools();
		var randomString         = testUserAccountTools.randomLongString();
		var userId               = generateUser();
		// Once we have generated an user, we can generate the ACL for the object
		var aclId = generateAcl(userId, null, true, true, true, true);

		var unit = Unit.builder()
					   .aclId(aclId)
					   .description("ITC generated unit")
					   .name("Test unit " + randomString)
					   .locked(false)
					   .created(Instant.now())
					   .creator(userId)
					   .modified(null)
					   .modified(null)
					   .build();
		var newUnit = unitService.save(unit);
		return newUnit.getId();
	}
	/////////////////// Unit end

	/////////////////// Gallery start
	public Long generateGalleryFromDirectory(long playerId) {
		var randomImagePath = RandomStringUtils.randomAlphanumeric(8);
		var testGalleryName = "Test gallery " + randomImagePath;

		var destinationFileList = prepareConvertedForTests(randomImagePath);

		var gallery = fileService.createEmptyGallery(testGalleryName, "A test gallery generated by TestITCTools", playerId);
		fileService.addFilesToDatabase("/Test/" + randomImagePath, destinationFileList, playerId, gallery.getId());

		var optionalGallery = galleryRepository.findByShortname(testGalleryName);
		assertTrue(optionalGallery.isPresent());
		log.info("Generated gallery: {}", optionalGallery.get());
		return optionalGallery.get()
							  .getId();
	}

	public List<Path> prepareConvertedForTests(String randomImagePath) {
		var destinationFileList = new ArrayList<Path>();
		// Get list of files in service/src/test/resources/files
		var fileList = new ArrayList<Path>();
		try {
			try (Stream<Path> files = Files.walk(Path.of("src/test/resources/files"))
										   .filter(Files::isRegularFile)) {
				files.forEach(fileList::add);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// The copy the files in fileList to the destinationDir
		for (Path file : fileList) {
			var metaTool = new MetadataTools();
			// Get metadata
			var metadata   = metaTool.getMetadataAsJSON(file.toFile());
			var jsonArray  = new JSONArray(metadata);
			var jsonObject = jsonArray.getJSONObject(0);
			var mimetype   = JsonTools.extractMimetype(jsonObject);
			var fileClass  = getFileClassByMimetype(mimetype);
			var destinationFile = Path.of(convertedDirectory + File.separator + fileClass.name()
																						 .toLowerCase() + File.separator +
										  randomImagePath + File.separator + file.getFileName());
			destinationFileList.add(destinationFile);
			// Check if destinationFileDir exists, if not create it
			if (!Files.exists(destinationFile.getParent())) {
				try {
					Files.createDirectories(destinationFile.getParent());
				} catch (IOException e) {
					fail("Failed to create directory " + destinationFile.getParent() + ": " + e);
				}
			}

			// Make sure destinationFile does not exist
			if (Files.exists(destinationFile)) {
				try {
					Files.delete(destinationFile);
				} catch (IOException e) {
					fail("Failed to delete file " + destinationFile + ": " + e);
				}
			}

			try {
				Files.copy(file, destinationFile);
			} catch (IOException e) {
				fail("Failed to copy file " + file + " to " + destinationFile + ": " + e);
			}
		}

		return destinationFileList;
	}

	/////////////////// Gallery end
	///////////////////  start
	///////////////////  end
}
