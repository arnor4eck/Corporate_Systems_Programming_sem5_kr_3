package com.arnor4eck.pks_3.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.jayway.jsonpath.JsonPath;


class PlotApiTest extends CrudApiTest {
    @Test
    void rejectsDuplicateLocation() throws Exception {
        String body = input();
        create(path(), body);
        mvc.perform(post(path())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void cannotDeleteSectorWithPlots() throws Exception {
        String body = input();
        create(path(), body);
        String sectorId = JsonPath.read(body, "$.sectorId");
        String url = "/api/sectors/" + sectorId;
        mvc.perform(delete(url)
                        )
                .andExpect(status().isNoContent());
        mvc.perform(delete(url + "/permanent")
                        )
                .andExpect(status().isConflict());
    }

    @Override protected String path() { return "/api/plots"; }
    @Override protected String input() throws Exception {
        String sectorId = create("/api/sectors", """
                {"name":"Central"}
                """);
        return """
                {"sectorId":"%s","rowNumber":1,"plotNumber":2,"plotStatus":"FREE","lengthCm":200,"widthCm":100,"coordinates":null}
                """.formatted(sectorId);
    }
    @Override protected String updatedInput() throws Exception {
        return input().replace("\"FREE\"", "\"RESERVED\"");
    }
    @Override protected String changedField() { return "plotStatus"; }
    @Override protected Object changedValue() { return "RESERVED"; }

    @Test
    void rejectsMissingReference() throws Exception {
        String body = input().replaceFirst("\"sectorId\":\"[^\"]+\"", "\"sectorId\":\"" + java.util.UUID.randomUUID() + "\"");
        mvc.perform(post(path())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }
}
