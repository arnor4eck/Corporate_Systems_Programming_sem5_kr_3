package com.arnor4eck.pks_3.api;

import com.arnor4eck.pks_3.controller.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiExceptionHandlerTest {
    private MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }
    @RestController
    static class FailingController {
        @GetMapping("/failure") String failure() throws Exception { throw new Exception("Test operation failed"); }
        @GetMapping("/empty") String empty() { throw new IllegalStateException(); }
        @GetMapping("/missing") String missing() { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record missing"); }
        @GetMapping("/conflict") String conflict() { throw new DataIntegrityViolationException("duplicate"); }
        @GetMapping("/uuid/{id}") String uuid(@PathVariable("id") UUID id) { return id.toString(); }
    }
    @Test void returnsExceptionInformationAsProblemDetail() throws Exception {
        mvc.perform(get("/failure")).andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Test operation failed"))
                .andExpect(jsonPath("$.exception").value("Exception"))
                .andExpect(jsonPath("$.instance").value("/failure"));
    }
    @Test void providesFallbackForMissingMessage() throws Exception {
        mvc.perform(get("/empty")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected server error"))
                .andExpect(jsonPath("$.exception").value("IllegalStateException"));
    }
    @Test void preserves404() throws Exception {
        mvc.perform(get("/missing")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Record missing"));
    }
    @Test void preserves400() throws Exception {
        mvc.perform(get("/uuid/invalid")).andExpect(status().isBadRequest());
    }
    @Test void preservesConflictHandler() throws Exception {
        mvc.perform(get("/conflict")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
