package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.*;
import com.arnor4eck.pks_3.dto.*;
import com.arnor4eck.pks_3.dto.CsvDtos.*;
import com.arnor4eck.pks_3.security.TokenService;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigDecimal;
import java.nio.*;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CsvService {
    private final SearchService search;
    private final CustomerService customers;
    private final EmployeeService employees;
    private final SectorService sectors;
    private final PlotService plots;
    private final RequestService requests;
    private final AuditService audit;
    private final Validator validator;
    private final EntityManager em;
    private final PlatformTransactionManager transactions;
    private final byte[] previewKey = randomKey();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> appliedTokens = new java.util.concurrent.ConcurrentHashMap<>();
    public static final int MAX_ROWS = 5000;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private record Row(long number, UUID id, Status status, Object input, List<UUID> tags, List<String> errors) {}

    public byte[] export(ResourceType type, SearchQuery query) {
        long total = search.count(type, query);
        if (total > MAX_ROWS) throw SearchService.bad("Export exceeds 5000 rows; narrow the filters");
        var rows = search.rows(type, query, 0, MAX_ROWS);
        var output = new StringWriter();
        try (var printer = new CSVPrinter(output, CSVFormat.RFC4180)) {
            var components = responseClass(type).getRecordComponents();
            printer.printRecord(Arrays.stream(components).map(java.lang.reflect.RecordComponent::getName).toList());
            for (Object row : rows) {
                var values = new ArrayList<String>();
                for (var field : components) {
                    Object value = field.getAccessor().invoke(row);
                    if (row instanceof RequestResponse request && field.getName().equals("tags"))
                        value = request.tags().stream().map(t -> t.id().toString()).collect(java.util.stream.Collectors.joining("|"));
                    values.add(escapeCell(value == null ? "" : value.toString()));
                }
                printer.printRecord(values);
            }
        } catch (ReflectiveOperationException | IOException exception) { throw new IllegalStateException("CSV export failed", exception); }
        audit.record("CSV_EXPORT", type.name(), null, "Exported " + rows.size() + " rows");
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    public Preview preview(ResourceType type, byte[] bytes, Mode mode) {
        var rows = read(type, bytes, mode);
        var errors = rows.stream().filter(r -> !r.errors().isEmpty()).map(r -> new RowError(r.number(), r.errors())).toList();
        audit.record("CSV_PREVIEW", type.name(), null, "Checked " + rows.size() + " rows; invalid: " + errors.size());
        return new Preview(rows.size(), rows.size() - errors.size(), errors.size(), errors, token(type, bytes, mode));
    }

    public Applied apply(ResourceType type, byte[] bytes, Mode mode, String token) {
        long expiresAt = verify(type, bytes, mode, token);
        appliedTokens.entrySet().removeIf(entry -> entry.getValue() <= Instant.now().getEpochSecond());
        if (appliedTokens.putIfAbsent(TokenService.hash(token), expiresAt) != null)
            throw SearchService.bad("Preview was already applied; run preview again");
        var rows = read(type, bytes, mode); // Recheck references, constraints and permissions at application time.
        var results = new ArrayList<RowResult>();
        var tx = new TransactionTemplate(transactions);
        for (Row row : rows) {
            if (!row.errors().isEmpty()) {
                results.add(new RowResult(row.number(), false, row.id(), String.join("; ", row.errors())));
                continue;
            }
            try {
                UUID id = tx.execute(status -> save(type, row));
                results.add(new RowResult(row.number(), true, id, null));
            } catch (ResponseStatusException exception) {
                results.add(new RowResult(row.number(), false, row.id(), exception.getReason()));
            } catch (org.springframework.dao.DataIntegrityViolationException | org.springframework.dao.OptimisticLockingFailureException exception) {
                results.add(new RowResult(row.number(), false, row.id(), "Unique constraint or reference conflict"));
            }
        }
        int succeeded = (int) results.stream().filter(RowResult::success).count();
        audit.record("CSV_IMPORT", type.name(), null, "Imported " + succeeded + " rows; failed: " + (rows.size() - succeeded));
        return new Applied(rows.size(), succeeded, rows.size() - succeeded, results);
    }

    private List<Row> read(ResourceType type, byte[] bytes, Mode mode) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw SearchService.bad("CSV must contain 1 byte to 2 MiB");
        final String csv;
        try { csv = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString().replaceFirst("^\uFEFF", ""); }
        catch (CharacterCodingException exception) { throw SearchService.bad("CSV must use UTF-8"); }
        var allowed = new HashSet<String>();
        Arrays.stream(responseClass(type).getRecordComponents()).forEach(c -> allowed.add(c.getName()));
        if (type == ResourceType.employees) allowed.add("password");
        var format = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true)
                .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).get();
        try (var parser = CSVParser.parse(csv, format)) {
            var headers = parser.getHeaderNames();
            if (headers.isEmpty() || headers.stream().anyMatch(h -> h.isBlank() || !allowed.contains(h)))
                throw SearchService.bad("Unknown or missing CSV column; use the exported header names");
            var rows = new ArrayList<Row>();
            var ids = new HashSet<UUID>();
            var uniqueKeys = new HashSet<String>();
            for (var record : parser) {
                if (rows.size() >= MAX_ROWS) throw SearchService.bad("CSV exceeds 5000 rows");
                var errors = new ArrayList<String>();
                UUID id = null;
                Status archive = null;
                Object input = null;
                List<UUID> tagIds = null;
                try {
                    if (!record.isConsistent()) throw new IllegalArgumentException("Wrong number of columns");
                    var values = new HashMap<String, String>();
                    record.toMap().forEach((key, value) -> values.put(key, unescapeCell(value)));
                    id = uuid(values, "id");
                    if (type == ResourceType.requests && mode == Mode.UPSERT && value(values, "code") != null) {
                        var cb = em.getCriteriaBuilder(); var lookup = cb.createQuery(Request.class); var root = lookup.from(Request.class);
                        lookup.select(root).where(cb.equal(root.get("code"), value(values, "code")));
                        var existing = em.createQuery(lookup).getResultList().stream().findFirst();
                        if (existing.isPresent()) {
                            if (id != null && !id.equals(existing.get().getId())) errors.add("id and external code identify different records");
                            id = existing.get().getId();
                        }
                    }
                    archive = enumValue(values, "status", Status.class);
                    input = input(type, values);
                    if (type == ResourceType.requests && values.containsKey("tags")) {
                        String raw = values.get("tags");
                        tagIds = raw.isBlank() ? List.of() : Arrays.stream(raw.split("\\|", -1)).map(UUID::fromString).distinct().toList();
                        if (tagIds.size() > 50) errors.add("At most 50 tags per request");
                        for (UUID tagId : tagIds) reference(Tag.class, tagId, "tags", errors);
                    }
                    validator.validate(input).stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().forEach(errors::add);
                    if (id != null && !ids.add(id)) errors.add("Duplicate id in CSV");
                    boolean exists = id != null && em.find(type.entityClass, id) != null;
                    if (exists && value(values, "version") == null) errors.add("version is required for an update");
                    if (exists && value(values, "version") != null) {
                        Object stored = em.find(type.entityClass, id);
                        Long current = switch(stored) {
                            case Customer e -> e.getVersion(); case Employee e -> e.getVersion(); case Sector e -> e.getVersion();
                            case Plot e -> e.getVersion(); case Request e -> e.getVersion(); default -> throw new IllegalStateException();
                        };
                        if (!Objects.equals(current, version(values))) errors.add("Record version has changed; export or reload current data");
                    }
                    if (mode == Mode.NEW_ONLY && id != null) errors.add("NEW_ONLY requires an empty id; UUID is assigned by the server");
                    if (mode == Mode.UPSERT && id != null && !exists) errors.add("Update id does not exist; leave id empty to create");
                    if (type == ResourceType.employees && id == null && ((EmployeeInput) input).password() == null) errors.add("password: temporary password is required for a new employee");
                    if (input instanceof EmployeeInput e && e.password() != null && (e.password().isBlank() || e.password().getBytes(StandardCharsets.UTF_8).length > 72)) errors.add("password: invalid length or blank value");
                    references(input, errors);
                    String uniqueKey = switch (input) {
                        case EmployeeInput e -> e.login();
                        case PlotInput p -> p.sectorId() + "/" + p.rowNumber() + "/" + p.plotNumber();
                        case RequestInput r -> r.code();
                        default -> null;
                    };
                    if (uniqueKey != null && !uniqueKeys.add(uniqueKey)) errors.add("Duplicate login or plot location in CSV");
                    if (input instanceof RequestInput r && r.code() != null) {
                        var cb = em.getCriteriaBuilder(); var q = cb.createQuery(Long.class); var root = q.from(Request.class);
                        q.select(cb.count(root)).where(id == null ? cb.equal(root.get("code"), r.code()) : cb.and(cb.equal(root.get("code"), r.code()), cb.notEqual(root.get("id"), id)));
                        if (em.createQuery(q).getSingleResult() > 0) errors.add("External code already exists");
                    }
                    if (input instanceof EmployeeInput e && e.login() != null) {
                        var cb = em.getCriteriaBuilder(); var q = cb.createQuery(Long.class); var root = q.from(Employee.class);
                        q.select(cb.count(root)).where(id == null ? cb.equal(root.get("login"), e.login()) : cb.and(cb.equal(root.get("login"), e.login()), cb.notEqual(root.get("id"), id)));
                        if (em.createQuery(q).getSingleResult() > 0) errors.add("login already exists");
                    }
                    if (input instanceof PlotInput p && p.sectorId() != null && p.rowNumber() != null && p.plotNumber() != null) {
                        var cb = em.getCriteriaBuilder(); var q = cb.createQuery(Long.class); var root = q.from(Plot.class);
                        var same = cb.and(cb.equal(root.get("sector").get("id"), p.sectorId()), cb.equal(root.get("rowNumber"), p.rowNumber()), cb.equal(root.get("plotNumber"), p.plotNumber()));
                        q.select(cb.count(root)).where(id == null ? same : cb.and(same, cb.notEqual(root.get("id"), id)));
                        if (em.createQuery(q).getSingleResult() > 0) errors.add("plot location already exists");
                    }
                } catch (IllegalArgumentException | DateTimeException exception) { errors.add("Invalid UUID, enum, number, date or column count"); }
                rows.add(new Row(record.getRecordNumber() + 1, id, archive, input, tagIds, List.copyOf(errors)));
            }
            if (rows.isEmpty()) throw SearchService.bad("CSV contains no data rows");
            return rows;
        } catch (IOException | IllegalArgumentException | UncheckedIOException exception) { throw SearchService.bad("Malformed CSV or duplicate headers"); }
    }

    private void references(Object input, List<String> errors) {
        if (input instanceof PlotInput p) reference(Sector.class, p.sectorId(), "sectorId", errors);
        if (input instanceof RequestInput r) {
            reference(Customer.class, r.customerId(), "customerId", errors);
            reference(Plot.class, r.plotId(), "plotId", errors);
            if (r.employeeId() != null) reference(Employee.class, r.employeeId(), "employeeId", errors);
        }
    }
    private void reference(Class<?> type, UUID id, String field, List<String> errors) {
        if (id != null && em.find(type, id) == null) errors.add(field + ": record does not exist");
    }
    private Object input(ResourceType type, Map<String, String> v) {
        return switch (type) {
            case customers -> new CustomerInput(value(v, "fullName"), value(v, "phone"), value(v, "email"), version(v));
            case sectors -> new SectorInput(value(v, "name"), bool(v, "enabled"), version(v));
            case employees -> new EmployeeInput(value(v, "fullName"), enumValue(v, "role", Role.class), value(v, "login"), value(v, "password"), bool(v, "active"), version(v));
            case plots -> new PlotInput(uuid(v, "sectorId"), number(v, "rowNumber"), number(v, "plotNumber"), enumValue(v, "plotStatus", PlotStatus.class), number(v, "lengthCm"), number(v, "widthCm"), value(v, "coordinates"), version(v));
            case requests -> new RequestInput(uuid(v, "customerId"), uuid(v, "plotId"), uuid(v, "employeeId"), value(v, "deceasedFullName"), date(v, "deceasedBirthday"), date(v, "deceasedDeathday"), value(v, "deceasedCertificate"), enumValue(v, "requestStatus", RequestStatus.class), dateTime(v, "requestDate"), dateTime(v, "processedDate"), value(v, "totalCost") == null ? null : new BigDecimal(value(v, "totalCost")), value(v, "note"), value(v, "code"), value(v, "title"), bool(v, "urgent"), version(v));
        };
    }
    private UUID save(ResourceType type, Row row) {
        boolean create = row.id() == null;
        Object response = switch (type) {
            case customers -> create ? customers.create((CustomerInput)row.input()) : customers.update(row.id(), (CustomerInput)row.input());
            case employees -> create ? employees.create((EmployeeInput)row.input()) : employees.update(row.id(), (EmployeeInput)row.input());
            case sectors -> create ? sectors.create((SectorInput)row.input()) : sectors.update(row.id(), (SectorInput)row.input());
            case plots -> create ? plots.create((PlotInput)row.input()) : plots.update(row.id(), (PlotInput)row.input());
            case requests -> create ? requests.create((RequestInput)row.input()) : requests.update(row.id(), (RequestInput)row.input());
        };
        UUID id = switch (response) {
            case CustomerResponse r -> r.id(); case EmployeeResponse r -> r.id(); case SectorResponse r -> r.id();
            case PlotResponse r -> r.id(); case RequestResponse r -> r.id(); default -> throw new IllegalStateException();
        };
        if (type == ResourceType.requests && row.tags() != null) {
            var request = em.find(Request.class, id); request.getTags().clear();
            for (UUID tagId : row.tags()) {
                var tag = em.find(Tag.class, tagId);
                if (tag == null) throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Tag no longer exists");
                request.getTags().add(tag);
            }
            em.flush(); audit.record("UPDATE_TAGS", "requests", id, "CSV request labels updated");
        }
        if (row.status() != null) {
            // Import status changes are audited just like ordinary archive/restore requests.
            var entity = (com.arnor4eck.pks_3.domain.Entity<?>) em.find(type.entityClass, id);
            if (entity.getStatus() != row.status()) {
                if (row.status() == Status.ARCHIVE) archive(type, id); else restore(type, id);
            }
        }
        return id;
    }
    private void archive(ResourceType type, UUID id) {
        switch(type) { case customers -> customers.archive(id); case employees -> employees.archive(id); case sectors -> sectors.archive(id); case plots -> plots.archive(id); case requests -> requests.archive(id); }
    }
    private void restore(ResourceType type, UUID id) {
        switch(type) { case customers -> customers.restore(id); case employees -> employees.restore(id); case sectors -> sectors.restore(id); case plots -> plots.restore(id); case requests -> requests.restore(id); }
    }
    private Class<?> responseClass(ResourceType type) {
        return switch(type) { case customers -> CustomerResponse.class; case employees -> EmployeeResponse.class; case sectors -> SectorResponse.class; case plots -> PlotResponse.class; case requests -> RequestResponse.class; };
    }
    private String token(ResourceType type, byte[] bytes, Mode mode) {
        String payload = type + "|" + mode + "|" + actor() + "|" + hash(bytes) + "|" + Instant.now().plusSeconds(900).getEpochSecond() + "|" + UUID.randomUUID();
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + signature(encoded);
    }
    private long verify(ResourceType type, byte[] bytes, Mode mode, String token) {
        try {
            if (token == null || token.length() > 1000) throw new IllegalArgumentException();
            String[] pieces = token.split("\\.");
            if (pieces.length != 2 || !MessageDigest.isEqual(signature(pieces[0]).getBytes(StandardCharsets.US_ASCII), pieces[1].getBytes(StandardCharsets.US_ASCII))) throw new IllegalArgumentException();
            String[] fields = new String(Base64.getUrlDecoder().decode(pieces[0]), StandardCharsets.UTF_8).split("\\|");
            if (fields.length != 6 || !fields[0].equals(type.name()) || !fields[1].equals(mode.name()) || !fields[2].equals(actor()) || !fields[3].equals(hash(bytes)) || Long.parseLong(fields[4]) <= Instant.now().getEpochSecond()) throw new IllegalArgumentException();
            return Long.parseLong(fields[4]);
        } catch (IllegalArgumentException exception) { throw SearchService.bad("Preview token expired or does not match the file, mode or employee; run preview again"); }
    }
    private String actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return TokenService.hash(auth.getPrincipal() instanceof Employee e ? e.getId().toString() : auth.getName());
    }
    private String signature(String payload) {
        try { var mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(previewKey, "HmacSHA256")); return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException exception) { throw new IllegalStateException(exception); }
    }
    private static byte[] randomKey() { byte[] key = new byte[32]; new SecureRandom().nextBytes(key); return key; }
    private String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private static String value(Map<String,String> v, String key) { String s = v.get(key); return s == null || s.isEmpty() ? null : s; }
    private static UUID uuid(Map<String,String> v, String key) { return value(v,key) == null ? null : UUID.fromString(value(v,key)); }
    private static Integer number(Map<String,String> v, String key) { return value(v,key) == null ? null : Integer.valueOf(value(v,key)); }
    private static Long version(Map<String,String> v) { return value(v,"version") == null ? null : Long.valueOf(value(v,"version")); }
    private static LocalDate date(Map<String,String> v, String key) { return value(v,key) == null ? null : LocalDate.parse(value(v,key)); }
    private static LocalDateTime dateTime(Map<String,String> v, String key) { return value(v,key) == null ? null : LocalDateTime.parse(value(v,key)); }
    private static Boolean bool(Map<String,String> v, String key) { String s=value(v,key); if(s==null)return null; if(!s.equals("true")&&!s.equals("false"))throw new IllegalArgumentException(); return Boolean.valueOf(s); }
    private static <E extends Enum<E>> E enumValue(Map<String,String> v, String key, Class<E> type) { return value(v,key)==null ? null : Enum.valueOf(type,value(v,key)); }
    private static String escapeCell(String value) { return value.startsWith("'") || (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0))>=0) ? "'"+value : value; }
    private static String unescapeCell(String value) { return value.length()>1 && value.charAt(0)=='\'' && "'=+-@\t\r".indexOf(value.charAt(1))>=0 ? value.substring(1) : value; }
}
