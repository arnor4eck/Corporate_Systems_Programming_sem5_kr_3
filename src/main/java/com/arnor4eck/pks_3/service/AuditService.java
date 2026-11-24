package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.*;
import com.arnor4eck.pks_3.repository.AuditEventRepository;
import com.arnor4eck.pks_3.dto.*;
import com.arnor4eck.pks_3.filter.CorrelationIdFilter;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditEventRepository repository;

    @Transactional
    public void record(String action, String type, UUID id, String description) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Employee actor = authentication != null && authentication.getPrincipal() instanceof Employee e ? e : null;
        String login = authentication == null ? "system" : authentication.getName();
        write(actor == null ? null : actor.getId(), actor == null ? login : actor.getLogin(), action, type, id, description);
    }

    @Transactional
    public void authentication(Employee actor, String login, boolean success) {
        write(actor == null ? null : actor.getId(), login, success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE", "employees",
                actor == null ? null : actor.getId(), success ? "Employee authenticated" : "Authentication rejected");
    }

    @Transactional
    public void session(Employee actor, String action, String description) {
        write(actor.getId(), actor.getLogin(), action, "employees", actor.getId(), description);
    }

    private void write(UUID actorId, String login, String action, String type, UUID id, String description) {
        var event = new AuditEvent();
        event.setActorId(actorId);
        event.setActorLogin(login.length() > 50 ? login.substring(0, 50) : login);
        event.setAction(action);
        event.setEntityType(type);
        event.setEntityId(id);
        event.setCorrelationId(CorrelationIdFilter.current());
        event.setDescription(description);
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditDtos.Event> search(AuditDtos.Query f) {
        if (f.from() != null && f.to() != null && f.from().isAfter(f.to())) throw SearchService.bad("from exceeds to");
        String field = f.sort() == null ? "occurredAt" : f.sort();
        if (!Set.of("occurredAt", "actorLogin", "action", "entityType").contains(field)) throw SearchService.bad("Unsupported audit sort field");
        String direction = f.direction() == null ? "desc" : f.direction();
        if (!Set.of("asc", "desc").contains(direction)) throw SearchService.bad("Invalid sort direction");
        var pageable = PageRequest.of(f.page() == null ? 0 : f.page(), f.size() == null ? 20 : f.size(),
                Sort.by(Sort.Direction.fromString(direction), field).and(Sort.by("id")));
        var page = repository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (f.action() != null) predicates.add(cb.equal(root.get("action"), f.action()));
            if (f.entityType() != null) predicates.add(cb.equal(root.get("entityType"), f.entityType()));
            if (f.entityId() != null) predicates.add(cb.equal(root.get("entityId"), f.entityId()));
            if (f.actorId() != null) predicates.add(cb.equal(root.get("actorId"), f.actorId()));
            if (f.correlationId() != null) predicates.add(cb.equal(root.get("correlationId"), f.correlationId()));
            if (f.from() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), f.from()));
            if (f.to() != null) predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), f.to()));
            if (f.q() != null && !f.q().isBlank()) {
                String value = "%" + f.q().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("actorLogin")), value, '\\'), cb.like(cb.lower(root.get("description")), value, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable);
        return new PageResponse<>(page.stream().map(e -> new AuditDtos.Event(e.getId(), e.getActorId(), e.getActorLogin(), e.getAction(),
                e.getEntityType(), e.getEntityId(), e.getOccurredAt(), e.getCorrelationId(), e.getDescription())).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
