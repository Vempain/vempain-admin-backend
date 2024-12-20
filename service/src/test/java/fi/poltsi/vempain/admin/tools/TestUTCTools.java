package fi.poltsi.vempain.admin.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.api.request.AclRequest;
import fi.poltsi.vempain.admin.api.request.ComponentRequest;
import fi.poltsi.vempain.admin.api.request.LayoutRequest;
import fi.poltsi.vempain.admin.api.request.PageRequest;
import fi.poltsi.vempain.admin.api.response.AclResponse;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.FormComponent;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.entity.Unit;
import fi.poltsi.vempain.admin.entity.UserAccount;
import fi.poltsi.vempain.admin.entity.UserUnit;
import fi.poltsi.vempain.admin.entity.UserUnitId;
import fi.poltsi.vempain.admin.entity.file.FileCommon;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static fi.poltsi.vempain.tools.LocalFileTools.createAndVerifyDirectory;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class TestUTCTools {

	// Acl
	public static Acl generateAcl(Long id, long aclId, Long userId, Long unitId) {
		return Acl.builder()
				  .id(id)
				  .aclId(aclId)
				  .userId(userId)
				  .unitId(unitId)
				  .readPrivilege(true)
				  .createPrivilege(true)
				  .modifyPrivilege(true)
				  .deletePrivilege(true)
				  .build();
	}

	public static List<Acl> generateAclList(long aclId, Long count, boolean units) {
		ArrayList<Acl> acls = new ArrayList<>();

		Long id = 1L;

		for (long i = 1L; i <= count; i++) {
			Acl aclUser = generateAcl(id, aclId, i, null);
			acls.add(aclUser);
			id++;

			if (units) {
				Acl aclUnit = generateAcl(id, aclId, null, i);
				acls.add(aclUnit);
				id++;
			}
		}

		return acls;
	}

	public static List<Acl> generateAclList(long aclId, Long count) {
		return generateAclList(aclId, count, true);
	}

	public static List<AclRequest> generateAclRequestList(long aclId, Long count) {
		List<Acl> acls = generateAclList(aclId, count);
		return generateAclRequestListFromAcl(acls);
	}

	public static List<AclRequest> generateAclRequestListFromAcl(List<Acl> acls) {
		ArrayList<AclRequest> requests = new ArrayList<>();

		for (Acl acl : acls) {
			requests.add(AclRequest.builder()
								   .id(acl.getId())
								   .aclId(acl.getAclId())
								   .user(acl.getUserId())
								   .unit(acl.getUnitId())
								   .readPrivilege(acl.isReadPrivilege())
								   .modifyPrivilege(acl.isModifyPrivilege())
								   .createPrivilege(acl.isCreatePrivilege())
								   .deletePrivilege(acl.isDeletePrivilege())
								   .build());
		}

		return requests;
	}

	public static List<AclResponse> generateAclResponses(long aclId, long count) {
		ArrayList<AclResponse> aclResponses = new ArrayList<>();
		List<Acl>              acls         = generateAclList(aclId, count);

		for (Acl acl : acls) {
			aclResponses.add(acl.toResponse());
		}

		return aclResponses;
	}

	// Component
	public static Component generateComponent(long componentId, long aclId) {
		return Component.builder()
						.id(componentId)
						.compName("Test component " + componentId)
						.compData("Component data " + componentId)
						.locked(false)
						.aclId(aclId)
						.creator(1L)
						.created(Instant.now().minus(1, ChronoUnit.HOURS))
						.modifier(1L)
						.modified(Instant.now())
						.build();
	}

	public static List<Component> generateComponentList(long count) {
		ArrayList<Component> components = new ArrayList<>();

		for (long i = 0; i < count; i++) {
			components.add(generateComponent((i + 1), (i + 1)));
		}

		return components;
	}

	public static ComponentRequest generateComponentRequest(long componentId, long aclId) {
		Component component = generateComponent(componentId, aclId);
		return generateComponentRequestFromComponent(component);
	}

	public static ComponentRequest generateComponentRequestFromComponent(Component component) {
		return ComponentRequest.builder()
							   .id(component.getId())
							   .compName(component.getCompName())
							   .compData(component.getCompData())
							   .locked(component.isLocked())
							   .timestamp(LocalDateTime.now())
							   .acls(generateAclRequestList(component.getAclId(), 1L))
							   .build();
	}

	public static List<ComponentRequest> generateComponentRequestList(long count) {
		ArrayList<ComponentRequest> requests   = new ArrayList<>();
		List<Component>             components = generateComponentList(count);

		for (Component component : components) {
			requests.add(generateComponentRequestFromComponent(component));
		}

		return requests;
	}

	public static List<Long> generateComponentIdList(long count) {
		ArrayList<Long> componentIdList = new ArrayList<>();

		for (long i = 1; i <= count; i++) {
			componentIdList.add(i);
		}

		return componentIdList;
	}

	// Form
	public static Form generateForm(long formId, long aclId) {
		return Form.builder()
				   .id(formId)
				   .formName("Test form " + formId)
				   .layoutId(1L)
				   .aclId(aclId)
				   .locked(false)
				   .creator(1L)
				   .created(Instant.now().minus(1, ChronoUnit.HOURS))
				   .modifier(1L)
				   .modified(Instant.now())
				   .build();
	}

	public static List<Form> generateFormList(long count) {
		ArrayList<Form> forms = new ArrayList<>();
		for (long i = 1; i <= count; i++) {
			forms.add(generateForm(i, i));
		}

		return forms;
	}

	// FormComponent
	public static FormComponent generateFormComponent(long formId, long sortOrder) {
		var component = generateComponent(sortOrder, sortOrder);
		return FormComponent.builder()
							.formId(formId)
							.componentId(component.getId())
							.sortOrder(sortOrder)
							.build();
	}

	public static List<FormComponent> generateFormComponentList(long formId, long componentCount) {
		ArrayList<FormComponent> formComponents = new ArrayList<>();

		for (long i = 1; i <= componentCount; componentCount++) {
			formComponents.add(generateFormComponent(formId, i));
		}

		return formComponents;
	}

	// UserUnit
	public static UserUnit generateUserUnit(long userId, long unitId) {
		return UserUnit.builder()
					   .id(generateUserUnitId(userId, unitId))
					   .user(generateUser(userId))
					   .unit(generateUnit(unitId))
					   .build();
	}

	// Unit
	public static Unit generateUnit(long unitId) {
		return Unit.builder()
				   .id(unitId)
				   .name("Test unit " + unitId)
				   .build();
	}

	public static List<Unit> generateUnitList(long count) {
		ArrayList<Unit> units = new ArrayList<>();
		for (long i = 1; i <= count; i++) {
			units.add(generateUnit(i));
		}

		return units;
	}

	// User
	public static UserAccount generateUser(long userId) {
		log.info("Creating user with ID: {}", userId);
		return UserAccount.builder()
						  .id(userId)
						  .build();
	}

	public static List<UserAccount> generateUserList(long count) {
		ArrayList<UserAccount> users = new ArrayList<>();
		for (long i = 1; i <= count; i++) {
			users.add(generateUser(i));
		}

		return users;
	}

	// UserUnitId
	public static UserUnitId generateUserUnitId(long userId, long unitId) {
		return UserUnitId.builder()
						 .userId(userId)
						 .unitId(unitId)
						 .build();
	}

	// Page
	public static Page generatePage(long index) {
		return Page.builder()
				   .id(index)
				   .parentId(1L)
				   .header("Test header")
				   .title("Test title")
				   .formId(1L)
				   .indexList(false)
				   .secure(true)
				   .path("/index")
				   .body("This is test body")
				   .locked(false)
				   .aclId(index)
				   .creator(1L)
				   .created(Instant.now().minus(1, ChronoUnit.HOURS))
				   .modifier(1L)
				   .modified(Instant.now())
				   .build();
	}

	public static List<Page> generatePageList(long count) {
		ArrayList<Page> pages = new ArrayList<>();

		for (long i = 1; i <= count; i++) {
			pages.add(generatePage(i));
		}

		return pages;
	}

	public static PageRequest generatePageRequestFromPage(Page page) {
		return PageRequest.builder()
						  .id(page.getId())
						  .parentId(page.getParentId())
						  .formId(page.getFormId())
						  .path(page.getPath())
						  .secure(page.isSecure())
						  .indexList(page.isIndexList())
						  .title(page.getTitle())
						  .header(page.getHeader())
						  .body(page.getBody())
						  .acls(generateAclRequestList(page.getAclId(), 1L))
						  .timestamp(LocalDateTime.now())
						  .build();
	}

	public static PageRequest generatePageRequest(long pageId) {
		return generatePageRequestFromPage(generatePage(pageId));
	}

	// Layout
	public static Layout generateLayout(long layoutId) {
		return Layout.builder()
					 .id(layoutId)
					 .layoutName("Test layout " + layoutId)
					 .structure("layout structure" + layoutId)
					 .locked(false)
					 .aclId(layoutId)
					 .creator(1L)
					 .created(Instant.now().minus(1, ChronoUnit.HOURS))
					 .modifier(1L)
					 .modified(Instant.now())
					 .build();
	}

	public static List<Layout> generateLayoutList(long count) {
		ArrayList<Layout> layouts = new ArrayList<>();

		for (long i = 1; i <= count; i++) {
			layouts.add(generateLayout(i));
		}

		return layouts;
	}

	public static LayoutRequest generateLayoutRequest(Layout layout) {
		List<AclRequest> acls = generateAclRequestList(layout.getAclId(), 2L);

		return LayoutRequest.builder()
							.id(layout.getId())
							.layoutName(layout.getLayoutName())
							.structure(layout.getStructure())
							.acls(acls)
							.timestamp(LocalDateTime.now())
							.build();
	}

	public static List<LayoutResponse> generateLayoutResponseList(long count) {
		List<Layout>         layouts         = generateLayoutList(count);
		List<LayoutResponse> layoutResponses = new ArrayList<>();

		for (Layout layout : layouts) {
			layoutResponses.add(layout.getLayoutResponse());
		}

		return layoutResponses;
	}

	////////////////// FILE

	// FileCommon

	// Create a converted file from a test file in our resources

	public static FileCommon generateFileCommon(long fileCommonId, String storageDirSetup) {
		// The original file that we use as test material
		var originalFile = Path.of("src/test/resources/files/Norja-2019-0097.jpeg");

		if (!Files.exists(originalFile)) {
			log.error("Original file does not exist: {}", originalFile);
			return null;
		}

		var storageDir = Path.of(storageDirSetup);

		// Give the file a random name
		var randomFileName = RandomStringUtils.randomAlphabetic(12) + ".jpeg";
		var convertedPath = FileClassEnum.getFileClassNameByMimetype("image/jpeg")
							+ File.separator + RandomStringUtils.randomAlphabetic(12);
		var absoluteConvertedPath = Path.of(storageDir + File.separator + convertedPath);
		var convertedFile         = Path.of(convertedPath + File.separator + randomFileName);
		var absoluteConvertedFile = Path.of(storageDir + File.separator + convertedFile);
		log.info("OriginalFile: {}", originalFile);
		log.info("StorageDir: {}", storageDir);
		log.info("ConvertedPath: {}", convertedPath);
		log.info("ConvertedFile: {}", convertedFile);

		try {
			if (!Files.exists(absoluteConvertedPath)) {
				log.info("Creating converted directory: {}", absoluteConvertedPath);
				createAndVerifyDirectory(absoluteConvertedPath);
			}
			if (!Files.exists(absoluteConvertedFile)) {
				log.info("Copying image file from resource {} as test converted file: {}", originalFile, absoluteConvertedFile);
				var result = Files.copy(originalFile, absoluteConvertedFile);
				log.info("Result after copying: {}", result);
				log.info("Does the copied file exist now? {}", Files.exists(result));
			} else {
				log.error("Converted file already exists: {}", absoluteConvertedFile);
				throw new IOException("Converted file already exists");
			}
		} catch (IOException e) {
			log.error("Failed to copy image file from resource as test converted file: {}", absoluteConvertedPath, e);
			return null;
		}

		// The convertedFile should be relative to the main convertedDirectory, this is setting the absolute path
		return FileCommon.builder()
						 .id(fileCommonId)
						 .mimetype("image/jpeg")
						 .convertedFile(convertedFile.toString())
						 .convertedFilesize(100L + fileCommonId)
						 .convertedSha1sum("so-Sha1sum" + fileCommonId)
						 .siteFilename(originalFile.getFileName().toString())
						 .siteFilepath("site/")
						 .siteSha1sum("si-Sha1sum" + fileCommonId)
						 .comment("Test FileCommon")
						 .creator(1L)
						 .created(Instant.now().minus(1, ChronoUnit.HOURS))
						 .build();
	}

	// Generic
	public static <T> T deepCopy(T original, Class<T> classType) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
		try {
			return objectMapper.readValue(objectMapper.writeValueAsString(original), classType);
		} catch (JsonProcessingException e) {
			fail("Failed to deep copy FormComponent: " + e.getMessage());
			return null;
		}
	}
}
