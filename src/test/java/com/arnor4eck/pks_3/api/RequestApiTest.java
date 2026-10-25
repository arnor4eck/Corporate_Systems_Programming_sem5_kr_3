package com.arnor4eck.pks_3.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


class RequestApiTest extends CrudApiTest {
    @Override protected String path() { return "/api/requests"; }
    @Override protected String input() throws Exception {
        String customerId = create("/api/customers", """
                {"fullName":"Customer","phone":"123"}
                """);
        String sectorId = create("/api/sectors", """
                {"name":"Central"}
                """);
        String plotId = create("/api/plots", """
                {"sectorId":"%s","rowNumber":1,"plotNumber":2,"plotStatus":"FREE","lengthCm":200,"widthCm":100}
                """.formatted(sectorId));
        return """
                {"customerId":"%s","plotId":"%s","employeeId":null,"requestStatus":"NEW",
                 "requestDate":"2026-10-25T12:00:00","totalCost":100.50,"note":null}
                """.formatted(customerId, plotId);
    }
    @Override protected String updatedInput() throws Exception {
        return input().replace("\"NEW\"", "\"APPROVED\"");
    }
    @Override protected String changedField() { return "requestStatus"; }
    @Override protected Object changedValue() { return "APPROVED"; }

    @Test
    void rejectsMissingReference() throws Exception {
        String body = input().replaceFirst("\"customerId\":\"[^\"]+\"", "\"customerId\":\"" + java.util.UUID.randomUUID() + "\"");
        mvc.perform(post(path())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }
}
