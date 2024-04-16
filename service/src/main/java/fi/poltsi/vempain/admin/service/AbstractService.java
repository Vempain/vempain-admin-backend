package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.entity.AbstractVempainEntity;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public abstract class AbstractService {
    protected final AclService aclService;
    protected final AccessService accessService;

    protected AbstractService(AclService aclService, AccessService accessService) {
        this.aclService = aclService;
        this.accessService = accessService;
    }

    protected void validateAbstractData(AbstractVempainEntity entity) throws VempainAbstractException {
        if (entity.getAclId() < 1L) {
            log.error("Invalid ACL ID: {}", entity.getAclId());
            throw new VempainAbstractException("ACL ID is invalid");
        }

        if (entity.getCreator() == null ||
            entity.getCreator() < 1L) {
            log.error("Invalid creator ID: {}", entity.getCreator());
            throw new VempainAbstractException("Creator is missing or invalid");
        }

        if (entity.getCreated() == null) {
            log.error("Missing creation date");
            throw new VempainAbstractException("Created datetime is missing");
        }

        if (entity.getModifier() == null &&
            entity.getModified() != null) {
            log.error("Entity has modified date set, but not modifier");
            throw new VempainAbstractException("Modifier is missing while modified is set");
        }

        if (entity.getModifier() != null &&
            entity.getModified() == null) {
            log.error("Entity has modifier set, but not modified date");
            throw new VempainAbstractException("Modified datetime is missing while modifier is set");
        }

        if (entity.getModifier() != null &&
            entity.getModifier() < 1L) {
            log.error("Entity modifier is set but invalid: {}", entity.getModifier());
            throw new VempainAbstractException("Entity modifier is invalid");
        }

        if (entity.getModified() != null &&
            entity.getModified().isBefore(entity.getCreated())) {
            log.error("Entity modified date {} is before created date {}", entity.getModified(), entity.getCreated());
            throw new VempainAbstractException("Created datetime is more recent than modified");
        }
    }

    protected long getUserId() {
        try {
            return accessService.getUserId();
        } catch (SessionAuthenticationException e) {
            log.error(VempainMessages.INVALID_USER_SESSION_MSG);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.INVALID_USER_SESSION);
        }
    }
}
