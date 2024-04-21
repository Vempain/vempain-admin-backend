package fi.poltsi.vempain.admin.security;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityConfigITC {
    @Autowired
    private MockMvc mockMvc;

    @Value("${vempain.devel:false}")
    private boolean develEnv;

    @Test
    void configureOk() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/content-management/layouts"))
                                  .andExpect(status().is4xxClientError())
                                  .andReturn();
        log.info("Env setting for devel: {}", develEnv);
    }
}
