package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.request.LoginRequest;
import fi.poltsi.vempain.admin.api.response.JwtResponse;
import fi.poltsi.vempain.admin.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginControllerITC extends AbstractITCTest {
	private   User             testUser;
	private   String           testPassword;
	@Autowired
	protected TestRestTemplate testRestTemplate;

	@Test
	@DisplayName(Constants.LOGIN_PATH + " REST API test with correct credentials")
	void loginOk() {
		testPassword = testUserAccountTools.randomLongString();
		var userId       = testITCTools.generateUser();
		var optionalUser = userService.findById(userId);
		assertTrue(optionalUser.isPresent());
		testUser = optionalUser.get();
		testUser.setPassword(testUserAccountTools.encryptPassword(testPassword));
		userService.save(testUser);

		ResponseEntity<JwtResponse> response = getLoginResponse(this.testUser.getLoginName(), this.testPassword);

		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());

		JwtResponse jwtResponse = response.getBody();

		assertNotNull(jwtResponse);
		assertNotNull(jwtResponse.getToken());

		System.out.println("jwtResponse: " + jwtResponse);

		Assertions.assertEquals(this.testUser.getEmail(), jwtResponse.getEmail());
		Assertions.assertEquals(this.testUser.getLoginName(), jwtResponse.getLogin());
		assertTrue(jwtResponse.getUnits().isEmpty());
	}

	@Test
	@DisplayName(Constants.LOGIN_PATH + " REST API test with incorrect credentials")
	void loginFailed() {
		var userId       = testITCTools.generateUser();
		var optionalUser = userService.findById(userId);
		assertTrue(optionalUser.isPresent());
		this.testUser = optionalUser.get();

		ResponseEntity<JwtResponse> response =
				getLoginResponse(this.testUser.getLoginName(),
								 "wrong password");

		assertNotNull(response);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		JwtResponse jwtResponse = response.getBody();
		assertNull(jwtResponse);
	}

	private ResponseEntity<JwtResponse> getLoginResponse(String username,
														 String password) {
		URI targetURL = UriComponentsBuilder.fromUriString(Constants.LOGIN_PATH)
											.build()
											.encode()
											.toUri();

		LoginRequest loginRequest = LoginRequest.builder()
												.login(username)
												.password(password)
												.build();
		return testRestTemplate.postForEntity(targetURL,
											  loginRequest,
											  JwtResponse.class);
	}
}
