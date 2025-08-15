package fi.poltsi.vempain.admin.principal;

import fi.poltsi.vempain.auth.entity.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class VempainUserPrincipal implements UserDetails {
	private final UserAccount userAccount;

	public VempainUserPrincipal(UserAccount userAccount) {
		this.userAccount = userAccount;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return null;
	}

	@Override
	public String getPassword() {
		return userAccount.getPassword();
	}

	@Override
	public String getUsername() {
		return userAccount.getLoginName();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
