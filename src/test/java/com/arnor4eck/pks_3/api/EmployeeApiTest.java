package com.arnor4eck.pks_3.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class EmployeeApiTest extends CrudApiTest {
    @Test
    void rejectsDuplicateLogin() throws Exception {
        create(path(), input());
        mvc.perform(post(path())
                        .contentType(MediaType.APPLICATION_JSON).content(input()))
                .andExpect(status().isConflict());
    }

    @Override protected String path() { return "/api/employees"; }
    @Override protected String input() throws Exception {
        return """
                {"fullName":"Employee","role":"MANAGER","login":"manager","passwordHash":"test-hash","active":true}
                """;
    }
    @Override protected String updatedInput() throws Exception {
        return """
                {"fullName":"Updated","role":"WORKER","login":"worker","passwordHash":"updated-hash","active":false}
                """;
    }
    @Override protected String changedField() { return "role"; }
    @Override protected Object changedValue() { return "WORKER"; }

    @Test
    void neverReturnsPasswordHash() throws Exception {
        String id = create(path(), input());
        mvc.perform(get(path() + "/" + id))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get(path()))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }
}
