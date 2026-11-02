package fi.poltsi.vempain.admin.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import fi.poltsi.vempain.admin.api.request.file.FileIngestAclRequest;
import fi.poltsi.vempain.admin.api.response.file.FileIngestAclResponse;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.service.UnitService;
import fi.poltsi.vempain.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The additional grantees another service names when it creates a resource here: the {@code acls} list of the file ingest ({@code FileIngestRequest}) and of
 * the data set requests ({@code DataRequest}). The creating account always owns every privilege on the resource's ACL; this service validates the extra user
 * and unit rows before anything is stored, writes them next to the owner's row and reads them back in request shape.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AclGranteeService {

    private final AclService aclService;
    private final UserService userService;
    private final UnitService unitService;

    /** A user account that may be granted privileges: active and not locked */
    public static boolean isGrantable(UserAccount user) {
        return user.getStatus() == AccountStatus.ACTIVE
               && !user.isLocked();
    }

    /**
     * Validates the additional grantees before anything is stored: every entry names either an existing, active and unlocked user account or an existing unit,
     * each grantee at most once, and grants at least one privilege. Null and empty lists are valid.
     *
     * @throws IllegalArgumentException when the list is invalid (answered as 400 by the controllers)
     */
    public void validate(List<FileIngestAclRequest> acls) {
        if (acls == null
            || acls.isEmpty()) {
            return;
        }

        var seenUserIds = new HashSet<Long>();
        var seenUnitIds = new HashSet<Long>();
        Set<Long> existingUnitIds = null;

        for (var acl : acls) {
            validateShape(acl);

            if (acl.getUserId() != null) {
                validateUserGrantee(acl.getUserId(), seenUserIds);
            } else {
                if (existingUnitIds == null) {
                    existingUnitIds = StreamSupport.stream(unitService.findAll()
                        .spliterator(), false)
                        .map(Unit::getId)
                        .collect(Collectors.toSet());
                }
                validateUnitGrantee(acl.getUnitId(), seenUnitIds, existingUnitIds);
            }
        }
    }

    /**
     * Adds the grantees to an existing ACL. A grantee (user or unit) that already has a row on the ACL gets its privileges replaced by the requested ones; the
     * owning account itself is skipped because it already holds every privilege. The entries have been checked by {@link #validate} before.
     */
    public void grant(long aclId, List<FileIngestAclRequest> acls, long ownerUserId) {
        if (aclId < 1
            || acls == null
            || acls.isEmpty()) {
            return;
        }

        upsert(aclId, acls, ownerUserId, new ArrayList<>(aclService.findAclByAclId(aclId)));
    }

    /**
     * Makes the given list the complete set of additional grantees of the ACL: rows of users and units not in the list are removed (the owner's row is kept),
     * the listed ones are added or updated like {@link #grant}. A null list leaves the ACL untouched; an empty list removes every additional grantee.
     */
    public void replace(long aclId, List<FileIngestAclRequest> acls, long ownerUserId) {
        if (aclId < 1
            || acls == null) {
            return;
        }

        var existingRows = new ArrayList<>(aclService.findAclByAclId(aclId));

        for (var row : List.copyOf(existingRows)) {
            if (Objects.equals(row.getUserId(), ownerUserId)) {
                continue;
            }
            var requested = acls.stream()
                .anyMatch(request -> matches(row, request));
            if (!requested) {
                log.debug("Removing {} from ACL {}", granteeOf(row), aclId);
                aclService.deleteById(row.getId());
                existingRows.remove(row);
            }
        }

        upsert(aclId, acls, ownerUserId, existingRows);
    }

    /**
     * The rows of an ACL other than the owner's own row, in the shape the requests take them, so that a client can show and resend them.
     */
    public List<FileIngestAclResponse> additionalGrantees(long aclId, long ownerUserId) {
        if (aclId < 1) {
            return List.of();
        }

        return aclService.findAclByAclId(aclId)
            .stream()
            .filter(row -> row.getUserId() != null
                           || row.getUnitId() != null)
            .filter(row -> row.getUserId() == null
                           || !Objects.equals(row.getUserId(), ownerUserId))
            .map(row -> FileIngestAclResponse.builder()
                .userId(row.getUserId())
                .unitId(row.getUnitId())
                .readPrivilege(row.isReadPrivilege())
                .createPrivilege(row.isCreatePrivilege())
                .modifyPrivilege(row.isModifyPrivilege())
                .deletePrivilege(row.isDeletePrivilege())
                .build())
            .toList();
    }

    private void upsert(long aclId, List<FileIngestAclRequest> acls, long ownerUserId, List<Acl> existingRows) {
        for (var request : acls) {
            if (request.getUserId() != null
                && request.getUserId() == ownerUserId) {
                continue;
            }

            var existing = existingRows.stream()
                .filter(row -> matches(row, request))
                .findFirst();

            try {
                if (existing.isPresent()) {
                    var row = existing.get();
                    row.setReadPrivilege(request.isReadPrivilege());
                    row.setCreatePrivilege(request.isCreatePrivilege());
                    row.setModifyPrivilege(request.isModifyPrivilege());
                    row.setDeletePrivilege(request.isDeletePrivilege());
                    aclService.update(row);
                } else {
                    var row = aclService.save(Acl.builder()
                        .aclId(aclId)
                        .userId(request.getUserId())
                        .unitId(request.getUnitId())
                        .readPrivilege(request.isReadPrivilege())
                        .createPrivilege(request.isCreatePrivilege())
                        .modifyPrivilege(request.isModifyPrivilege())
                        .deletePrivilege(request.isDeletePrivilege())
                        .build());
                    existingRows.add(row);
                }
            } catch (VempainAclException e) {
                throw new IllegalStateException("Failed to grant " + granteeOf(request) + " privileges on ACL " + aclId, e);
            }
        }
    }

    private static boolean matches(Acl row, FileIngestAclRequest request) {
        return request.getUserId() != null ? Objects.equals(row.getUserId(), request.getUserId()) : Objects.equals(row.getUnitId(), request.getUnitId());
    }

    /** An entry names exactly one grantee with a positive ID and grants at least one privilege */
    private static void validateShape(FileIngestAclRequest acl) {
        if (acl == null
            || !acl.isUserOrUnit()
            || (acl.getUserId() != null
                && acl.getUserId() < 1)
            || (acl.getUnitId() != null
                && acl.getUnitId() < 1)) {
            throw new IllegalArgumentException("ACL entry must name either a valid user ID or a valid unit ID");
        }

        if (!acl.isReadPrivilege()
            && !acl.isCreatePrivilege()
            && !acl.isModifyPrivilege()
            && !acl.isDeletePrivilege()) {
            throw new IllegalArgumentException("ACL entry for " + granteeOf(acl) + " grants no privilege");
        }
    }

    private void validateUserGrantee(long userId, Set<Long> seenUserIds) {
        if (!seenUserIds.add(userId)) {
            throw new IllegalArgumentException("ACL entry for user " + userId + " is listed more than once");
        }

        var user = userService.findById(userId);

        if (user.isEmpty()
            || !isGrantable(user.get())) {
            throw new IllegalArgumentException("ACL entry refers to an unknown or inactive user " + userId);
        }
    }

    private static void validateUnitGrantee(long unitId, Set<Long> seenUnitIds, Set<Long> existingUnitIds) {
        if (!seenUnitIds.add(unitId)) {
            throw new IllegalArgumentException("ACL entry for unit " + unitId + " is listed more than once");
        }

        if (!existingUnitIds.contains(unitId)) {
            throw new IllegalArgumentException("ACL entry refers to an unknown unit " + unitId);
        }
    }

    private static String granteeOf(FileIngestAclRequest acl) {
        return acl.getUserId() != null ? "user " + acl.getUserId() : "unit " + acl.getUnitId();
    }

    private static String granteeOf(Acl row) {
        return row.getUserId() != null ? "user " + row.getUserId() : "unit " + row.getUnitId();
    }
}
