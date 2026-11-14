package com.arnor4eck.pks_3.api;

import com.arnor4eck.pks_3.domain.*;
import com.arnor4eck.pks_3.repository.*;
import com.arnor4eck.pks_3.security.TokenService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {
    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository employees;
    @Autowired RefreshSessionRepository sessions;
    @Autowired PasswordEncoder passwords;
    private static final String PASSWORD = "InitialPassword123!";

    @AfterEach
    void clean() { sessions.deleteAll(); employees.deleteAll(); }

    private Employee employee(String login, Role role, boolean temporary) {
        var e = new Employee();
        e.setLogin(login);
        e.setFullName("Test employee");
        e.setRole(role);
        e.setPasswordHash(passwords.encode(PASSWORD));
        e.setPasswordChangeRequired(temporary);
        return employees.saveAndFlush(e);
    }

    private ResultActions json(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private String login(String login) throws Exception {
        return json("/api/auth/login", "{\"login\":\"" + login + "\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
    }
    private String field(String json, String key) { return JsonPath.read(json, "$." + key); }
    private String refreshBody(String token) { return "{\"refreshToken\":\"" + token + "\"}"; }

    @Test
    void loginRotationLogoutAndMe() throws Exception {
        employee("manager", Role.MANAGER, false);
        String result = login("manager");
        String access = field(result, "accessToken");
        String refresh = field(result, "refreshToken");
        assertThat(access.split("\\.")).hasSize(3);
        assertThat(sessions.findAll().getFirst().getTokenHash()).isEqualTo(TokenService.hash(refresh)).isNotEqualTo(refresh);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.login").value("manager"));
        String rotated = json("/api/auth/refresh", refreshBody(refresh)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(field(rotated, "refreshToken")).isNotEqualTo(refresh);
        json("/api/auth/refresh", refreshBody(refresh)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
        json("/api/auth/logout", refreshBody(field(rotated, "refreshToken"))).andExpect(status().isNoContent());
        json("/api/auth/refresh", refreshBody(field(rotated, "refreshToken"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + field(rotated, "accessToken")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void temporaryPasswordRestrictsAccessAndChangingItRevokesAllSessions() throws Exception {
        var e = employee("admin", Role.ADMIN, true);
        String first = login("admin");
        String second = login("admin");
        String access = field(first, "accessToken");
        mvc.perform(get("/api/customers").header("Authorization", "Bearer " + access)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.passwordChangeRequired").value(true));
        mvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"currentPassword":"InitialPassword123!","newPassword":"ReplacementPassword123!"}
                        """))
                .andExpect(status().isNoContent());
        var saved = employees.findById(e.getId()).orElseThrow();
        assertThat(saved.isPasswordChangeRequired()).isFalse();
        assertThat(passwords.matches("ReplacementPassword123!", saved.getPasswordHash())).isTrue();
        json("/api/auth/refresh", refreshBody(field(second, "refreshToken"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
        json("/api/auth/login", """
                {"login":"admin","password":"ReplacementPassword123!"}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.user.passwordChangeRequired").value(false));
    }

    @Test
    void rolesProtectReadsWritesDictionaryAndAdministration() throws Exception {
        employee("worker", Role.WORKER, false);
        employee("manager", Role.MANAGER, false);
        employee("admin", Role.ADMIN, false);
        mvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
        String worker = field(login("worker"), "accessToken");
        String manager = field(login("manager"), "accessToken");
        String admin = field(login("admin"), "accessToken");
        mvc.perform(get("/api/customers").header("Authorization", "Bearer " + worker)).andExpect(status().isOk());
        mvc.perform(post("/api/customers").header("Authorization", "Bearer " + worker)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/customers").header("Authorization", "Bearer " + manager)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Test\",\"phone\":\"123\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/employees").header("Authorization", "Bearer " + manager)).andExpect(status().isForbidden());
        mvc.perform(post("/api/sectors").header("Authorization", "Bearer " + manager)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Test\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/customers/" + java.util.UUID.randomUUID() + "/permanent")
                .header("Authorization", "Bearer " + manager)).andExpect(status().isForbidden());
        mvc.perform(get("/api/employees").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/employees?page=0&size=2").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/employees?page=0&size=101").header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disabledArchivedAndExpiredSessionsAreRejected() throws Exception {
        var e = employee("disabled", Role.MANAGER, false);
        String result = login("disabled");
        e.setActive(false);
        e = employees.saveAndFlush(e);
        json("/api/auth/login", "{\"login\":\"disabled\",\"password\":\"" + PASSWORD + "\"}").andExpect(status().isUnauthorized());
        json("/api/auth/refresh", refreshBody(field(result, "refreshToken"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + field(result, "accessToken"))).andExpect(status().isUnauthorized());
        e.setActive(true);
        e.setStatus(Status.ARCHIVE);
        e = employees.saveAndFlush(e);
        json("/api/auth/login", "{\"login\":\"disabled\",\"password\":\"" + PASSWORD + "\"}").andExpect(status().isUnauthorized());
        e.setStatus(Status.ACTIVE);
        e = employees.saveAndFlush(e);
        var session = sessions.findAll().getFirst();
        session.setExpiresAt(Instant.now().minusSeconds(10));
        sessions.saveAndFlush(session);
        json("/api/auth/refresh", refreshBody(field(result, "refreshToken"))).andExpect(status().isUnauthorized());
    }

    @Test
    void failedLoginLimitPersistsAndExpires() throws Exception {
        var e = employee("locked", Role.WORKER, false);
        for (int i = 0; i < 5; i++) {
            json("/api/auth/login", "{\"login\":\"locked\",\"password\":\"wrong\"}").andExpect(status().isUnauthorized());
        }
        var saved = employees.findById(e.getId()).orElseThrow();
        assertThat(saved.getFailedLogins()).isEqualTo(5);
        assertThat(saved.getLockedUntil()).isAfter(Instant.now());
        json("/api/auth/login", "{\"login\":\"locked\",\"password\":\"" + PASSWORD + "\"}").andExpect(status().isUnauthorized());
        saved.setLockedUntil(Instant.now().minusSeconds(1));
        employees.saveAndFlush(saved);
        login("locked");
        assertThat(employees.findById(e.getId()).orElseThrow().getFailedLogins()).isZero();
    }

    @Test
    void administratorCreatesAndResetsTemporaryCredentials() throws Exception {
        employee("admin", Role.ADMIN, false);
        String admin = field(login("admin"), "accessToken");
        String created = mvc.perform(post("/api/employees").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"login":"new-worker","fullName":"Worker","role":"WORKER","password":"InitialPassword123!","active":true}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
        String result = login("new-worker");
        var saved = employees.findByLogin("new-worker").orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2a$12$").isNotEqualTo(PASSWORD);
        assertThat(saved.isPasswordChangeRequired()).isTrue();
        mvc.perform(post("/api/employees/" + field(created, "id") + "/reset-password")
                        .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"temporaryPassword\":\"ResetPassword123!\"}"))
                .andExpect(status().isNoContent());
        json("/api/auth/refresh", refreshBody(field(result, "refreshToken"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + field(result, "accessToken")))
                .andExpect(status().isUnauthorized());
        json("/api/auth/login", "{\"login\":\"new-worker\",\"password\":\"ResetPassword123!\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.passwordChangeRequired").value(true));
    }

    @Test
    void rejectsInvalidInputAndForgedTokens() throws Exception {
        json("/api/auth/login", "{}").andExpect(status().isBadRequest());
        json("/api/auth/refresh", "{}").andExpect(status().isBadRequest());
        json("/api/auth/refresh", refreshBody("unknown")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
}
