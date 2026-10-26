package com.arnor4eck.pks_3.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


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
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    private void assertPlot(String id, String expected) throws Exception {
        mvc.perform(get("/api/plots/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.plotStatus").value(expected));
    }

    @Test
    void synchronizesPlotAcrossRequestLifecycleAndOtherActiveRequests() throws Exception {
        String body = input();
        String plot = com.jayway.jsonpath.JsonPath.read(body, "$.plotId");
        String id = create(path(), body);
        String url = path() + "/" + id;
        assertPlot(plot, "RESERVED");
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("NEW", "COMPLETED"))).andExpect(status().isOk());
        assertPlot(plot, "OCCUPIED");
        String another = create(path(), body);
        assertPlot(plot, "OCCUPIED");
        mvc.perform(delete(url)).andExpect(status().isNoContent());
        assertPlot(plot, "RESERVED");
        mvc.perform(post(url + "/restore")).andExpect(status().isOk());
        assertPlot(plot, "OCCUPIED");
        mvc.perform(delete(url)).andExpect(status().isNoContent());
        mvc.perform(delete(url + "/permanent")).andExpect(status().isNoContent());
        assertPlot(plot, "RESERVED");
        mvc.perform(put(path() + "/" + another).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("NEW", "REJECTED"))).andExpect(status().isOk());
        assertPlot(plot, "FREE");
    }

    @Test
    void movesReservationAndRollsBackOnInvalidReference() throws Exception {
        String body = input();
        String oldPlot = com.jayway.jsonpath.JsonPath.read(body, "$.plotId");
        String id = create(path(), body);
        String nextBody = input();
        String nextPlot = com.jayway.jsonpath.JsonPath.read(nextBody, "$.plotId");
        mvc.perform(put(path() + "/" + id).contentType(MediaType.APPLICATION_JSON).content(nextBody))
                .andExpect(status().isOk());
        assertPlot(oldPlot, "FREE"); assertPlot(nextPlot, "RESERVED");
        String invalid = body.replaceFirst("\"customerId\":\"[^\"]+\"",
                "\"customerId\":\"" + java.util.UUID.randomUUID() + "\"");
        mvc.perform(put(path() + "/" + id).contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isNotFound());
        assertPlot(oldPlot, "FREE"); assertPlot(nextPlot, "RESERVED");
        mvc.perform(get(path() + "/" + id)).andExpect(jsonPath("$.plotId").value(nextPlot));
    }

}
