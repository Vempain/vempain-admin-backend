package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.User;
import fi.poltsi.vempain.admin.repository.AclRepository;
import fi.poltsi.vempain.admin.repository.UserRepository;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class UserServiceUTC {
    @Mock
    private UserRepository userRepository;
	@Mock
	private AclRepository  aclRepository;
    private UserService    userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, aclRepository);
    }

    @Test
    void findAllOk() {
        List<User> users = TestUTCTools.generateUserList(10L);
        when(userRepository.findAll()).thenReturn(users);

        try {
            Iterable<User> returnValue = userService.findAll();
            assertNotNull(returnValue);
            assertEquals(10, StreamSupport.stream(returnValue.spliterator(), false).count());
        } catch (Exception e) {
            fail("We should not have received any exception");
        }
    }

    @Test
    void findByIdOk() {
        User user = TestUTCTools.generateUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        try {
            Optional<User> returnUser = userService.findById(1L);
            assertTrue(returnUser.isPresent());
            assertEquals(user, returnUser.get());
        } catch (Exception e) {
            fail("We should not have received any exception");
        }
    }

    @Test
    void lockByIdOk() {
        doNothing().when(userRepository).lockByUserId(1L);

        try {
            userService.lockUser(1L);
        } catch (Exception e) {
            fail("We should not have received any exception");
        }
    }
}
