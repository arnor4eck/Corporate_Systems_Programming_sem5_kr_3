package com.arnor4eck.pks_3.api;

import com.arnor4eck.pks_3.domain.Employee;
import com.arnor4eck.pks_3.domain.Role;
import com.arnor4eck.pks_3.repository.EmployeeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "springdoc.override-with-generic-response=false")
@AutoConfigureMockMvc
@Sql(statements = {"DELETE FROM refresh_sessions", "DELETE FROM requests", "DELETE FROM plots",
        "DELETE FROM employees", "DELETE FROM customers", "DELETE FROM sectors"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OpenApiTest {
    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository employees;
    @Autowired PasswordEncoder passwords;
    @Autowired com.arnor4eck.pks_3.repository.RefreshSessionRepository sessions;

    @Test
    void onlyAdministratorCanLoadDocumentation() throws Exception {
        for (String path : List.of("/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config", "/v3/api-docs.yaml")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(user("worker").roles("WORKER"))).andExpect(status().isForbidden());
            mvc.perform(get(path).with(user("manager").roles("MANAGER"))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/swagger-ui.html").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
    }

    @Test
    void browserLoginUsesExistingAdministratorAndDoesNotCreateRefreshSessions() throws Exception {
        var employee = new Employee();
        employee.setLogin("docs-admin");
        employee.setFullName("Documentation administrator");
        employee.setRole(Role.ADMIN);
        employee.setPasswordHash(passwords.encode("DocumentationPassword123!"));
        employee.setPasswordChangeRequired(false);
        employees.saveAndFlush(employee);
        mvc.perform(get("/v3/api-docs").with(httpBasic("docs-admin", "DocumentationPassword123!")))
                .andExpect(status().isOk());
        assertThat(sessions.count()).isZero();
        mvc.perform(get("/api/employees").with(httpBasic("docs-admin", "DocumentationPassword123!")))
                .andExpect(status().isUnauthorized());
        // Browsers may automatically attach cached documentation credentials on this same origin.
        mvc.perform(post("/api/auth/login").with(httpBasic("docs-admin", "DocumentationPassword123!"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"docs-admin\",\"password\":\"DocumentationPassword123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        employee.setPasswordChangeRequired(true);
        employees.saveAndFlush(employee);
        mvc.perform(get("/v3/api-docs").with(httpBasic("docs-admin", "DocumentationPassword123!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void specificationDescribesRealRoutesSchemasRolesAndJwt() throws Exception {
        String json = mvc.perform(get("/v3/api-docs").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("Система управления кладбищем — REST API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.schemas.LoginRequest.properties.password.writeOnly").value(true))
                .andExpect(jsonPath("$.components.schemas.EmployeeInput.properties.password.writeOnly").value(true))
                .andExpect(jsonPath("$.components.schemas.EmployeeResponse.properties.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.EmployeeResponse.properties.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(json, "$.paths");
        assertThat(paths).containsKeys("/api/auth/login", "/api/auth/me", "/api/requests", "/api/plots", "/api/sectors", "/api/customers", "/api/employees");
        assertThat(paths).containsKeys("/api/analytics/summary", "/api/admin/audit", "/api/requests/search", "/api/requests/export", "/api/requests/import/preview", "/api/requests/import/apply");
        for (var item : paths.values()) for (var operation : item.values()) {
            assertThat((String) operation.get("summary")).isNotBlank().doesNotContain("?");
            assertThat((Map<String, Object>) operation.get("responses")).containsKeys("400", "401", "403", "500");
        }
        var login = paths.get("/api/auth/login").get("post");
        assertThat((List<?>) login.get("security")).isEmpty();
        assertThat((String) login.get("summary")).contains("Вход");
        assertThat(paths.get("/api/requests/{id}/permanent").get("delete").get("summary").toString()).contains("ADMIN");
        assertThat(json).doesNotContain("\"passwordHash\"", "????");
        var employees = paths.get("/api/employees").get("get");
        List<Map<String, Object>> parameters = (List<Map<String, Object>>) employees.get("parameters");
        assertThat(parameters.stream().filter(p -> "page".equals(p.get("name"))).findFirst().orElseThrow().get("required")).isEqualTo(false);
        var me = paths.get("/api/auth/me").get("get");
        assertThat(me.get("parameters")).isNull();
    }
}
