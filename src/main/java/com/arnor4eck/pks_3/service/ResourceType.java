package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

public enum ResourceType {
    customers(Customer.class, List.of("fullName", "phone", "email"), Set.of("id", "status", "fullName", "phone", "email", "createdAt")),
    employees(Employee.class, List.of("fullName", "login"), Set.of("id", "status", "fullName", "login", "role", "active", "createdAt")),
    sectors(Sector.class, List.of("name"), Set.of("id", "status", "name", "createdAt")),
    plots(Plot.class, List.of("coordinates", "sector.name"), Set.of("id", "status", "sector.name", "rowNumber", "plotNumber", "lengthCm", "widthCm", "plotStatus")),
    requests(Request.class, List.of("code", "title", "deceasedFullName", "deceasedCertificate", "note", "customer.fullName", "customer.phone", "plot.sector.name"),
            Set.of("id", "status", "requestStatus", "requestDate", "processedDate", "totalCost", "createdAt", "updatedAt", "code", "title", "urgent", "deceasedFullName", "customer.fullName", "plot.sector.name"));

    public final Class<?> entityClass;
    public final List<String> textFields;
    public final Set<String> sortFields;
    ResourceType(Class<?> entityClass, List<String> textFields, Set<String> sortFields) {
        this.entityClass = entityClass; this.textFields = textFields; this.sortFields = sortFields;
    }
    public static ResourceType parse(String name) {
        try { return valueOf(name); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown resource"); }
    }
}
