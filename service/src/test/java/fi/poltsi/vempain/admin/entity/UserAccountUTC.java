package fi.poltsi.vempain.admin.entity;

import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import fi.poltsi.vempain.auth.entity.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAccountUTC {
    UserAccount userAccount;

    @BeforeEach
    void setUp() {
		userAccount = TestUTCTools.generateUser(1L);
    }

    @Test
    void getUserResponse() {
        UserResponse userResponse = userAccount.getUserResponse();
        assertEquals(1L, userResponse.getId());
    }
}
