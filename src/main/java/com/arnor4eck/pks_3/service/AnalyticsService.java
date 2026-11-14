package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.*;
import com.arnor4eck.pks_3.dto.AnalyticsResponse;
import com.arnor4eck.pks_3.dto.AnalyticsResponse.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {
    private final EntityManager em;

    public AnalyticsResponse summary() {
        var cb = em.getCriteriaBuilder();
        var money = cb.createTupleQuery();
        var requests = money.from(Request.class);
        money.multiselect(cb.sum(requests.<BigDecimal>get("totalCost")), cb.avg(requests.<BigDecimal>get("totalCost")))
                .where(cb.equal(requests.get("status"), Status.ACTIVE));
        var amounts = em.createQuery(money).getSingleResult();
        var employeeCounts = counts(Employee.class);
        var plotCounts = counts(Plot.class);
        long enabled = count(Employee.class, "active", true, true);
        long occupied = count(Plot.class, "plotStatus", PlotStatus.OCCUPIED, true);
        var percent = plotCounts.active() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(occupied).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(plotCounts.active()), 2, RoundingMode.HALF_UP);
        return new AnalyticsResponse(counts(Customer.class), employeeCounts, counts(Sector.class), plotCounts, counts(Request.class),
                enabled, employeeCounts.active() - enabled, decimal(amounts.get(0)), decimal(amounts.get(1)), percent,
                groups(Request.class, "requestStatus"), groups(Plot.class, "plotStatus"), sectors(),
                count(Request.class, "urgent", true, true), count(Request.class, "urgent", false, true), priorities());
    }

    private Counts counts(Class<?> type) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(type);
        query.select(cb.count(root));
        long total = em.createQuery(query).getSingleResult();
        long active = count(type, "status", Status.ACTIVE, false);
        return new Counts(total, active, total - active);
    }
    private long count(Class<?> type, String field, Object value, boolean activeOnly) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(type);
        var predicate = cb.equal(root.get(field), value);
        query.select(cb.count(root)).where(activeOnly ? cb.and(predicate, cb.equal(root.get("status"), Status.ACTIVE)) : predicate);
        return em.createQuery(query).getSingleResult();
    }
    private List<Group> groups(Class<?> type, String field) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createTupleQuery();
        var root = query.from(type);
        query.multiselect(root.get(field), cb.count(root)).where(cb.equal(root.get("status"), Status.ACTIVE)).groupBy(root.get(field)).orderBy(cb.asc(root.get(field)));
        var counts = new HashMap<String, Long>();
        em.createQuery(query).getResultList().forEach(t -> counts.put(t.get(0).toString(), (Long) t.get(1)));
        Object[] values = (type == Request.class ? RequestStatus.class : PlotStatus.class).getEnumConstants();
        return Arrays.stream(values).map(v -> new Group(v.toString(), v.toString(), counts.getOrDefault(v.toString(), 0L))).toList();
    }
    private List<Group> sectors() {
        var cb = em.getCriteriaBuilder();
        var query = cb.createTupleQuery();
        var root = query.from(Request.class);
        var sector = root.get("plot").get("sector");
        query.multiselect(sector.get("id"), sector.get("name"), cb.count(root)).where(cb.equal(root.get("status"), Status.ACTIVE))
                .groupBy(sector.get("id"), sector.get("name")).orderBy(cb.asc(sector.get("name")), cb.asc(sector.get("id")));
        return em.createQuery(query).getResultList().stream().map(t -> new Group(t.get(0).toString(), t.get(1).toString(), (Long)t.get(2))).toList();
    }
    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }
    private List<Group> priorities() {
        var cb = em.getCriteriaBuilder(); var query = cb.createTupleQuery(); var root = query.from(Request.class);
        query.multiselect(root.get("requestStatus"), root.get("urgent"), cb.count(root))
                .where(cb.equal(root.get("status"), Status.ACTIVE)).groupBy(root.get("requestStatus"), root.get("urgent"));
        var totals = new java.util.EnumMap<Priority, Long>(Priority.class);
        for (var tuple : em.createQuery(query).getResultList()) {
            var sample = new Request(); sample.setRequestStatus((RequestStatus) tuple.get(0)); sample.setUrgent((Boolean) tuple.get(1));
            totals.merge(sample.getPriority(), (Long) tuple.get(2), Long::sum);
        }
        return java.util.Arrays.stream(Priority.values()).map(p -> new Group(p.name(), p.name(), totals.getOrDefault(p, 0L))).toList();
    }
}
