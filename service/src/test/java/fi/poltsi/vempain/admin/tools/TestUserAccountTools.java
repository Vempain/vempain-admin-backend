package fi.poltsi.vempain.admin.tools;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestUserAccountTools {

	public String encryptPassword(String password) {
		BCryptPasswordEncoder pwdEncryptor = new BCryptPasswordEncoder(12);
		return pwdEncryptor.encode(password);
	}

	public String randomLongString() {
		return RandomStringUtils.randomAlphanumeric(14);
	}
}
