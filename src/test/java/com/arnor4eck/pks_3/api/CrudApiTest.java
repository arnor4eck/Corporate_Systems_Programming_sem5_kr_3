package com.arnor4eck.pks_3.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.jdbc.Sql;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {"DELETE FROM requests", "DELETE FROM plots", "DELETE FROM employees",
        "DELETE FROM customers", "DELETE FROM sectors"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@WithMockUser
abstract class CrudApiTest {
    @Autowired protected MockMvc mvc;
    protected abstract String path();
    protected abstract String input() throws Exception;
    protected abstract String updatedInput() throws Exception;
    protected abstract String changedField();
    protected abstract Object changedValue();

    protected String create(String path, String body) throws Exception {
        String json = mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void crudAndArchiveLifecycle() throws Exception {
        String id = create(path(), input());
        String url = path() + "/" + id;
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get(path())).andExpect(status().isOk()).andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.hasItem(id)));
        mvc.perform(delete(url + "/permanent").with(csrf())).andExpect(status().isConflict());
        mvc.perform(post(url + "/restore").with(csrf())).andExpect(status().isConflict());
        mvc.perform(put(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(updatedInput()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$." + changedField()).value(changedValue()));
        mvc.perform(delete(url).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(jsonPath("$.status").value("ARCHIVE"));
        mvc.perform(post(url + "/restore").with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(delete(url).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete(url + "/permanent").with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidInput() throws Exception {
        mvc.perform(post(path()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path() + "/invalid-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForMissingRecords() throws Exception {
        String url = path() + "/" + UUID.randomUUID();
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(put(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(input()))
                .andExpect(status().isNotFound());
        mvc.perform(delete(url).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(url + "/restore").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete(url + "/permanent").with(csrf())).andExpect(status().isNotFound());
    }
}
