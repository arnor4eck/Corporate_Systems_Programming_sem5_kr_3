package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.*;
import com.arnor4eck.pks_3.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {
    private final EntityManager em;
    private final CustomerService customers;
    private final EmployeeService employees;
    private final SectorService sectors;
    private final PlotService plots;
    private final RequestService requests;

    public PageResponse<Object> search(ResourceType type, SearchQuery filter) {
        long count = count(type, filter);
        return new PageResponse<>(rows(type, filter, filter.pageNumber() * filter.pageSize(), filter.pageSize()),
                filter.pageNumber(), filter.pageSize(), count, (int) ((count + filter.pageSize() - 1) / filter.pageSize()));
    }

    public long count(ResourceType type, SearchQuery filter) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(type.entityClass);
        query.select(cb.count(root)).where(predicates(type, filter, root, cb));
        return em.createQuery(query).getSingleResult();
    }

    public List<Object> rows(ResourceType type, SearchQuery filter, int offset, int limit) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Object.class);
        var root = query.from(type.entityClass);
        query.select(root).where(predicates(type, filter, root, cb));
        var orders = new ArrayList<Order>();
        List<String> sort = filter.sort() == null ? List.of() : filter.sort();
        // Spring may split a single sort=field,desc value into two list elements.
        if (sort.size() == 2 && List.of("asc", "desc").contains(sort.get(1).toLowerCase(Locale.ROOT))) sort = List.of(String.join(",", sort));
        if (sort.size() > 4) throw bad("At most four sort fields are allowed");
        for (String value : sort) {
            String[] parts = value.split(",", -1);
            if (parts.length > 2 || !type.sortFields.contains(parts[0])) throw bad("Unsupported sort field");
            boolean descending = parts.length == 2 && parts[1].equalsIgnoreCase("desc");
            if (parts.length == 2 && !descending && !parts[1].equalsIgnoreCase("asc")) throw bad("Sort direction must be asc or desc");
            orders.add(descending ? cb.desc(path(root, parts[0])) : cb.asc(path(root, parts[0])));
        }
        orders.add(cb.asc(root.get("id")));
        query.orderBy(orders);
        return em.createQuery(query).setFirstResult(offset).setMaxResults(limit).getResultList().stream().map(this::response).toList();
    }

    public Predicate[] predicates(ResourceType type, SearchQuery f, Root<?> root, CriteriaBuilder cb) {
        var result = new ArrayList<Predicate>();
        if (f.archiveStatus() != null) result.add(cb.equal(root.get("status"), f.archiveStatus()));
        equal(result, root, cb, f.code(), "code", type == ResourceType.requests);
        equal(result, root, cb, f.urgent(), "urgent", type == ResourceType.requests);
        equal(result, root, cb, f.enabled(), "enabled", type == ResourceType.sectors);
        if (f.priority() != null) {
            if (type != ResourceType.requests) throw bad("priority filter is only applicable to requests");
            result.add(cb.equal(priority(root, cb), f.priority().name()));
        }
        if (f.tagId() != null) {
            if (type != ResourceType.requests) throw bad("tagId filter is only applicable to requests");
            result.add(cb.equal(root.join("tags").get("id"), f.tagId()));
        }
        if (f.q() != null && !f.q().isBlank()) {
            String text = "%" + f.q().strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            result.add(cb.or(type.textFields.stream().map(field -> cb.like(cb.lower(path(root, field).as(String.class)), text, '\\')).toArray(Predicate[]::new)));
        }
        equal(result, root, cb, f.requestStatus(), "requestStatus", type == ResourceType.requests);
        equal(result, root, cb, f.plotStatus(), type == ResourceType.requests ? "plot.plotStatus" : "plotStatus", type == ResourceType.requests || type == ResourceType.plots);
        equal(result, root, cb, f.role(), "role", type == ResourceType.employees);
        equal(result, root, cb, f.active(), "active", type == ResourceType.employees);
        equal(result, root, cb, f.customerId(), "customer.id", type == ResourceType.requests);
        equal(result, root, cb, f.employeeId(), "employee.id", type == ResourceType.requests);
        equal(result, root, cb, f.plotId(), "plot.id", type == ResourceType.requests);
        equal(result, root, cb, f.sectorId(), type == ResourceType.requests ? "plot.sector.id" : "sector.id", type == ResourceType.requests || type == ResourceType.plots);
        equal(result, root, cb, f.rowNumber(), "rowNumber", type == ResourceType.plots);
        equal(result, root, cb, f.plotNumber(), "plotNumber", type == ResourceType.plots);
        range(result, root, cb, f.minCost(), f.maxCost(), "totalCost", type == ResourceType.requests);
        range(result, root, cb, f.minLengthCm(), f.maxLengthCm(), "lengthCm", type == ResourceType.plots);
        range(result, root, cb, f.fromDate(), f.toDate(), type == ResourceType.requests ? "requestDate" : "createdAt", type != ResourceType.plots);
        return result.toArray(Predicate[]::new);
    }

    private void equal(List<Predicate> result, Root<?> root, CriteriaBuilder cb, Object value, String field, boolean allowed) {
        if (value == null) return;
        if (!allowed) throw bad("Filter is not applicable to this resource: " + field);
        result.add(cb.equal(path(root, field), value));
    }
    private <T extends Comparable<? super T>> void range(List<Predicate> result, Root<?> root, CriteriaBuilder cb, T min, T max, String field, boolean allowed) {
        if (min == null && max == null) return;
        if (!allowed) throw bad("Filter is not applicable to this resource: " + field);
        if (min != null && max != null && min.compareTo(max) > 0) throw bad("Minimum exceeds maximum: " + field);
        if (min != null) result.add(cb.greaterThanOrEqualTo(path(root, field), min));
        if (max != null) result.add(cb.lessThanOrEqualTo(path(root, field), max));
    }
    @SuppressWarnings("unchecked")
    public static <T> Path<T> path(Path<?> root, String field) {
        Path<?> current = root;
        for (String part : field.split("\\.")) current = current.get(part);
        return (Path<T>) current;
    }
    public static Expression<String> priority(Root<?> root, CriteriaBuilder cb) {
        return cb.<String>selectCase()
                .when(root.get("requestStatus").in(RequestStatus.COMPLETED, RequestStatus.REJECTED), "NORMAL")
                .when(cb.isTrue(root.get("urgent")), "URGENT")
                .when(cb.equal(root.get("requestStatus"), RequestStatus.PROCESSING), "HIGH").otherwise("NORMAL");
    }
    private Object response(Object entity) {
        return switch (entity) {
            case Customer e -> customers.response(e);
            case Employee e -> employees.response(e);
            case Sector e -> sectors.response(e);
            case Plot e -> plots.response(e);
            case Request e -> requests.response(e);
            default -> throw new IllegalArgumentException("Unsupported entity");
        };
    }
    public static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
