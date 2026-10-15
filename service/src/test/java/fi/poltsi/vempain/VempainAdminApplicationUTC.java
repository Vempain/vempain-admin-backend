package fi.poltsi.vempain;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class VempainAdminApplicationUTC {

    @Test
    void mainStartsTheSpringApplicationWithTheGivenArguments() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            spring.when(() -> SpringApplication.run(eq(VempainAdminApplication.class), any(String[].class)))
                .thenReturn(mock(ConfigurableApplicationContext.class));

            VempainAdminApplication.main(new String[]{ "--server.port=0" });

            spring.verify(() -> SpringApplication.run(VempainAdminApplication.class, new String[]{ "--server.port=0" }));
        }
    }
}
