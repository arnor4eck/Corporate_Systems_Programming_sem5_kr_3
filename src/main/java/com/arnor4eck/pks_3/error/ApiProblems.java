package com.arnor4eck.pks_3.error;

import com.arnor4eck.pks_3.filter.CorrelationIdFilter;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import java.io.IOException;
import java.net.URI;

public final class ApiProblems {
    private ApiProblems() {}
    private static final tools.jackson.databind.json.JsonMapper JSON = tools.jackson.databind.json.JsonMapper.builder().build();
    public static ProblemDetail create(HttpStatusCode status, String detail, String path) {
        var result = ProblemDetail.forStatusAndDetail(status, detail);
        result.setType(URI.create("about:blank"));
        var known = HttpStatus.resolve(status.value());
        result.setTitle(known == null ? "HTTP error" : known.getReasonPhrase());
        result.setInstance(URI.create(path));
        result.setProperty("correlationId", CorrelationIdFilter.current());
        return result;
    }
    public static void write(HttpServletRequest request, HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status); response.setContentType("application/problem+json"); response.setCharacterEncoding("UTF-8");
        var problem = create(HttpStatusCode.valueOf(status), detail, request.getRequestURI());
        var body = new java.util.LinkedHashMap<String,Object>();
        body.put("type", problem.getType().toString()); body.put("title", problem.getTitle()); body.put("status", status);
        body.put("detail", detail); body.put("instance", request.getRequestURI()); body.put("correlationId", CorrelationIdFilter.current());
        response.getWriter().write(JSON.writeValueAsString(body));
    }
}
