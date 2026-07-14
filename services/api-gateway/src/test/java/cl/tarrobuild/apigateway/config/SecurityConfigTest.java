package cl.tarrobuild.apigateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void shouldAllowAuthLoginWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus(),
                        "Auth login should not require authentication"));
    }

    @Test
    void shouldAllowAuthRegisterWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/auth/register"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus(),
                        "Auth register should not require authentication"));
    }

    @Test
    void shouldAllowV1AuthLoginWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus(),
                        "V1 auth login should not require authentication"));
    }

    @Test
    void shouldAllowGetProductsPublicly() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus(),
                        "GET products should be public"));
    }

    @Test
    void shouldAllowV1GetProductsPublicly() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus(),
                        "GET v1 products should be public"));
    }

    @Test
    void shouldAllowGetCategoriesPublicly() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus(),
                        "GET categories should be public"));
    }

    @Test
    void shouldAllowHealthEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus(),
                        "Health endpoint should be public"));
    }

    @Test
    void shouldDenyGetBuildsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/builds"))
                .andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));
    }

    @Test
    void shouldDenyPostBuildsWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/builds"))
                .andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));
    }

    @Test
    void shouldDenyPostProductsWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/products"))
                .andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));
    }

    @Test
    void shouldDenyGetUsersWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));
    }

    @Test
    void shouldDenyPostUsersWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/users"))
                .andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));
    }

    @Test
    void shouldDenyPostCategoriesWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/categories"))
                .andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));
    }

    @Test
    void shouldDenyEstimateWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/estimate"))
                .andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));
    }

    @Test
    void shouldDenyProvidersWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/providers"))
                .andExpect(result -> assertNotEquals(200, result.getResponse().getStatus()));
    }
}
