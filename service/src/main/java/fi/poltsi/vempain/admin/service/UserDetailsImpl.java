package fi.poltsi.vempain.admin.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.poltsi.vempain.admin.entity.Unit;
import fi.poltsi.vempain.admin.entity.User;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class UserDetailsImpl implements UserDetails {
	@Serial
	private static final long serialVersionUID = 1L;
	private final Long id;
	private final String loginName;
	private final String nickName;
	private final String    email;
	private final Set<Unit> units;

	@JsonIgnore
	private final String password;

	private final Collection<? extends GrantedAuthority> authorities;

	public UserDetailsImpl(Long id, String loginName, String nickName, String email, String password, Set<Unit> units,
						   Collection<? extends GrantedAuthority> authorities) {
		this.id          = id;
		this.loginName   = loginName;
		this.nickName    = nickName;
		this.email       = email;
		this.password    = password;
		this.units       = units;
		this.authorities = authorities;
	}

	public static UserDetailsImpl build(User user) {
		List<GrantedAuthority> authorities = user.getUnits().stream()
												 .map(unit -> new SimpleGrantedAuthority(unit.getName()))
												 .collect(Collectors.toList());

		var units = user.getUnits();

		return new UserDetailsImpl(
				user.getId(),
				user.getLoginName(),
				user.getNick(),
				user.getEmail(),
				user.getPassword(),
				units,
				authorities);
	}

	@Override
	public String getUsername() {
		return loginName;
	}

	@Override
	public boolean isAccountNonExpired() {
		// TODO Implement
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// TODO Implement
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// TODO Implement
		return true;
	}

	@Override
	public boolean isEnabled() {
		// TODO Implement
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UserDetailsImpl user = (UserDetailsImpl) o;
		return Objects.equals(id, user.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, loginName, nickName, email, password, authorities);
	}
}
