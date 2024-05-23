package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.api.request.UserRequest;
import fi.poltsi.vempain.admin.api.response.UserResponse;
import fi.poltsi.vempain.admin.entity.UserAccount;
import fi.poltsi.vempain.admin.rest.UserAPI;
import fi.poltsi.vempain.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

// TODO Check that the user has permission to access this API

@Slf4j
@RequiredArgsConstructor
@RestController
public class UserController implements UserAPI {
	private final UserService userService;

	@Override
	public ResponseEntity<List<UserResponse>> getUsers() {
		Iterable<UserAccount> userAccounts = userService.findAll();

		ArrayList<UserResponse> responses = new ArrayList<>();

		for (var userAccount : userAccounts) {
			responses.add(userAccount.getUserResponse());
		}

		return ResponseEntity.ok(responses);
	}

	@Override
	public ResponseEntity<UserResponse> findById(Long userId) {
		if (userId == null || userId < 0) {
			log.error("Invalid unit ID: {}", userId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed parameter");
		}

		var userResponse = userService.findUserResponseById(userId);

		if (userResponse == null) {
			log.error("Could not find any unit by id {}", userId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user was found with given ID");
		}

		return ResponseEntity.ok(userResponse);
	}

	@Override
	public ResponseEntity<UserResponse> addUser(UserRequest userRequest) {
		var userResponse = userService.createUser(userRequest);

		return ResponseEntity.ok(userResponse);
	}

	@Override
	public ResponseEntity<UserResponse> updateUser(Long userId, UserRequest userRequest) {
		var userResponse = userService.updateUser(userId, userRequest);
		return ResponseEntity.ok(userResponse);
	}

	@ExceptionHandler(RuntimeException.class)
	public final ResponseEntity<Exception> handleRuntimeExceptions(RuntimeException e) {
		return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
