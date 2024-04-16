package fi.poltsi.vempain.admin.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class AuthenticationToken {
	final UUID          authToken;
	final LocalDateTime expiration;

	public AuthenticationToken() {
		this.authToken  = null;
		this.expiration = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
	}
}
