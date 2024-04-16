package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.entity.AbstractVempainEntity;
import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.entity.Unit;
import fi.poltsi.vempain.admin.entity.User;
import fi.poltsi.vempain.admin.entity.file.FileCommon;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.exception.VempainLayoutException;
import fi.poltsi.vempain.admin.service.AclService;
import fi.poltsi.vempain.admin.service.ComponentService;
import fi.poltsi.vempain.admin.service.FormService;
import fi.poltsi.vempain.admin.service.LayoutService;
import fi.poltsi.vempain.admin.service.PageService;
import fi.poltsi.vempain.admin.service.UnitService;
import fi.poltsi.vempain.admin.service.UserService;
import fi.poltsi.vempain.admin.service.file.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Slf4j
@AllArgsConstructor
@Service
public class AclConsistencySchedule {
	private static final long                             DELAY         = 60 * 60 * 1000L;
	private static final String                           INITIAL_DELAY = "#{ 30 * 1000 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(" + DELAY + ") }";
	private static final long                             OPERATOR_ID   = 1;
	private final        AclService                       aclService;
	private final        ComponentService                 componentService;
	private final        FormService                      formService;
	private final        LayoutService                    layoutService;
	private final        PageService                      pageService;
	private final        UnitService                      unitService;
	private final        UserService                      userService;
	private final        FileService                      fileService;
	private              Set<Long>                        tableAcls;
	private              Set<Long>                        missingAcls;
	private              Set<Long>                        orphanAcls;
	private              ArrayList<AbstractVempainEntity> duplicateAclObjects;

	@Scheduled(fixedDelay = DELAY, initialDelayString = INITIAL_DELAY)
	public void verify() {
		tableAcls = getAclSetFromTable();
		Set<Long> objectAcls = getAclSetFromServices();

		if (tableAcls.size() != objectAcls.size()) {
			log.error("The object and table sets are not equal in size");
		}

		if (!objectAcls.containsAll(tableAcls)) {
			log.error("Object set is missing some of the IDs listed in the table set");

			for (long aclId : tableAcls) {
				if (!objectAcls.contains(aclId)) {
					log.error("Acl {} is an orphan", aclId);
					orphanAcls.add(aclId);
				}
			}
		}

		if (!tableAcls.containsAll(objectAcls)) {
			log.error("Table set is missing some of the IDs listed in the object set");

			for (long aclId : objectAcls) {
				if (!tableAcls.contains(aclId)) {
					log.error("Acl {} used by an object is missing from acl table", aclId);
				}
			}
		}

		// Are there ACLs where both user and unit is defined in the same ACL
		deduplicateUserUnitAcls();
		// Are the duplicate objects with the same ACL ID
		deduplicateObjectAcls();
		// Create ACL with default permission missing from the table
		createMissingAcls();
		// Remove orhan ACLs
		removeOrphanAcls();
		// Are there duplicates where we have multiple rows with the same ACL ID, user ID or unit ID and (various) permission sets
		removeDuplicateAcls();
	}

	private void removeDuplicateAcls() {
		List<Long> duplicateAcls = aclService.findDuplicateAcls();

		if (duplicateAcls.isEmpty()) {
			log.info("No ACL duplicates found");
			return;
		}

		for (Long aclId : duplicateAcls) {
			List<Acl> aclList = aclService.findAclByAclId(aclId);

			if (aclList.size() < 2) {
				log.warn("There's something strange going on, we got duplicate ACLs for a ID ({}) of 1", aclId);
			} else {
				log.info("Removing duplicate ACLs for ACL ID {}", aclId);

				for (var i = 0; i < aclList.size(); i++) {
					for (var j = 0; j < aclList.size(); j++) {
						if (i != j) {
							var aclA = aclList.get(i);
							var aclB = aclList.get(j);
							if (aclA.equals(aclB)) {
								log.info("Removing duplicate ACL: {}", aclB);
								aclService.deleteByPermissionId(aclB.getPermissionId());
							}
						}
					}
				}
			}
		}
	}

	private void removeOrphanAcls() {
		for (Iterator<Long> itr = orphanAcls.iterator(); itr.hasNext(); ) {
			Long aclId = itr.next();
			log.info("Removing orphan ACL ID: {}", aclId);

			try {
				aclService.deleteByAclId(aclId);
				itr.remove();
			} catch (VempainEntityNotFoundException e) {
				log.error("Error removing orphaned ACL ID {}: {}", aclId, e.getMessage());
			}
		}
	}

	// This looks for ACL permissions where the row has both user both unit defined. This will be split by adding a new ACL
	// for the unit
	private void deduplicateUserUnitAcls() {
		Iterable<Acl> acls = aclService.findAllUserUnitAcls();

		if (acls.iterator().hasNext()) {
			for (Acl acl : acls) {
				long aclId  = acl.getAclId();
				long unitId = acl.getUnitId();

				// First we remove the unit ID
				acl.setUnitId(null);
				try {
					aclService.update(acl);
				} catch (VempainAclException e) {
					log.error("Failed to update ACL ID {} when splitting due to duplicate", aclId);
				}

				var unitAcl = Acl.builder()
								 .aclId(aclId)
								 .userId(null)
								 .unitId(unitId)
								 .createPrivilege(acl.isCreatePrivilege())
								 .modifyPrivilege(acl.isModifyPrivilege())
								 .readPrivilege(acl.isReadPrivilege())
								 .deletePrivilege(acl.isDeletePrivilege())
								 .build();
				try {
					aclService.save(unitAcl);
				} catch (VempainAclException e) {
					log.error("Failed to add ACL ID {} when adding the unit ACL", aclId);
				}

				acl.setUnitId(null);

				try {
					aclService.save(acl);
				} catch (VempainAclException e) {
					log.error("Failed to save original ACL with removed Unit ID:  ACL ID {}", aclId);
				}
			}
		}
	}

	// If we have multiple objects (page, form, layout... stored in the duplicateAclObjects variable) which have the same ACL, then we will
	// check if the ACL does exist. If it exists, then we will create a new ACL ID and copy the existing permissions. If the ACL ID does
	// not exist, then we will create a new ACL where the designated operator has full permissions
	private void deduplicateObjectAcls() {
		if (duplicateAclObjects.isEmpty()) {
			log.info("No objects have duplicate ACL ID, sleeping...");
			return;
		}

		for (AbstractVempainEntity entity : duplicateAclObjects) {
			// Get the next available ACL
			long      aclId        = aclService.getNextAclId();
			List<Acl> originalAcls = aclService.findAclByAclId(entity.getAclId());

			if (originalAcls.isEmpty()) {
				var newAcl = Acl.builder()
								.aclId(aclId)
								.userId(OPERATOR_ID)
								.unitId(null)
								.readPrivilege(true)
								.modifyPrivilege(true)
								.createPrivilege(true)
								.deletePrivilege(true)
								.build();
				try {
					aclService.save(newAcl);
				} catch (VempainAclException e) {
					log.error("Failed to create an ACL for object ({}) ID {}", entity.getClass().getName(), entity.getId());
				}
			} else {
				for (Acl acl : originalAcls) {
					var newAcl = Acl.builder()
									.aclId(aclId)
									.userId(acl.getUserId())
									.unitId(acl.getUnitId())
									.readPrivilege(acl.isReadPrivilege())
									.modifyPrivilege(acl.isModifyPrivilege())
									.createPrivilege(acl.isCreatePrivilege())
									.deletePrivilege(acl.isDeletePrivilege())
									.build();
					try {
						aclService.save(newAcl);
					} catch (VempainAclException e) {
						log.error("Failed to copy an ACL for object ({}) ID {}", entity.getClass().getName(), entity.getId());
					}
				}

				try {
					entity.setAclId(aclId);
					switch (entity) {
						case Component inst -> componentService.save(inst);
						case FileCommon inst -> {
							log.debug("Saving FileCommon with new ACL: {}", inst);
							fileService.saveFileCommon(inst);
						}
						case Form inst -> formService.save(inst);
						case Gallery inst -> fileService.saveGallery(inst);
						case Layout inst -> layoutService.save(inst);
						case Page inst -> pageService.save(inst);
						case Unit inst -> unitService.save(inst);
						case User inst -> userService.save(inst);
						default -> {
						}
					}

					log.info("Saved object ({}) ID {} with new ACL ID {}", entity.getClass(), entity.getId(), entity.getAclId());
				} catch (VempainLayoutException | VempainComponentException | VempainAbstractException e) {
					log.error("Failed to update the ACL ID {} of object {}", aclId, entity.getId());
				}
			}
		}
	}

	// We create missing ACLs with full default operator permissions. This may create duplicate ACLs, but that will be taken
	// care of in the next round of deduplication
	private void createMissingAcls() {
		for (Iterator<Long> itr = missingAcls.iterator(); itr.hasNext(); ) {
			Long aclId = itr.next();
			var newAcl = Acl.builder()
							.aclId(aclId)
							.userId(OPERATOR_ID)
							.unitId(null)
							.readPrivilege(true)
							.modifyPrivilege(true)
							.createPrivilege(true)
							.deletePrivilege(true)
							.build();
			try {
				log.info("Creating missing ACL: {}", newAcl);
				aclService.save(newAcl);
				itr.remove();
			} catch (VempainAclException e) {
				log.error("Failed to create an ACL for object using ACL ID {}", aclId);
			}
		}
	}

	private Set<Long> getAclSetFromTable() {
		Set<Long>     aclList = new HashSet<>();
		Iterable<Acl> acls    = aclService.findAll();

		for (Acl acl : acls) {
			aclList.add(acl.getAclId());
		}

		// We need to make this list unique
		return aclList;
	}

	private <T extends AbstractVempainEntity> Set<Long> getAclsFromObject(Iterable<T> objectList, String name) {
		Set<Long> acls = new HashSet<>();

		for (T object : objectList) {
			if (!acls.add(object.getAclId())) {
				log.error("The {} id {} tried to add a duplicate ACL ID {}", name, object.getId(), object.getAclId());
				duplicateAclObjects.add(object);
			}

			if (!tableAcls.contains(object.getAclId())) {
				log.error("The {} id {} ACL ID {} is missing from acl table", name, object.getId(), object.getAclId());
				missingAcls.add(object.getAclId());
			}
		}

		return acls;
	}

	protected Set<Long> getAclSetFromServices() {
		Set<Long> acls = new HashSet<>();
		acls.addAll(getAclsFromObject(componentService.findAll(), "component"));
		acls.addAll(getAclsFromObject(formService.findAll(), "form"));
		acls.addAll(getAclsFromObject(layoutService.findAll(), "layout"));
		acls.addAll(getAclsFromObject(pageService.findAll(), "page"));
		acls.addAll(getAclsFromObject(unitService.findAll(), "unit"));
		acls.addAll(getAclsFromObject(userService.findAll(), "user"));
		acls.addAll(getAclsFromObject(fileService.findAllFileCommon(), "file_common"));
		acls.addAll(getAclsFromObject(fileService.findAllGalleries(), "gallery"));

		return acls;
	}
}
