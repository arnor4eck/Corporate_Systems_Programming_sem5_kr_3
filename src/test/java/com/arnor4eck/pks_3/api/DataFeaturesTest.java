package com.arnor4eck.pks_3.api;

import com.arnor4eck.pks_3.domain.*;
import com.arnor4eck.pks_3.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
@Sql(statements = {"DELETE FROM audit_events", "DELETE FROM refresh_sessions", "DELETE FROM requests", "DELETE FROM plots", "DELETE FROM employees", "DELETE FROM customers", "DELETE FROM sectors"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {"DELETE FROM audit_events", "DELETE FROM refresh_sessions", "DELETE FROM requests", "DELETE FROM plots", "DELETE FROM employees", "DELETE FROM customers", "DELETE FROM sectors"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DataFeaturesTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository customers;
    @Autowired SectorRepository sectors;
    @Autowired PlotRepository plots;
    @Autowired RequestRepository requests;
    @Autowired EmployeeRepository employees;
    @Autowired AuditEventRepository audit;
    @Autowired PasswordEncoder passwords;

    private Customer customer(String name) {
        var c = new Customer(); c.setFullName(name); c.setPhone("+79990000000"); return customers.saveAndFlush(c);
    }
    private Sector sector(String name) { var s = new Sector(); s.setName(name); return sectors.saveAndFlush(s); }
    private Plot plot(Sector s, int number, PlotStatus status) {
        var p = new Plot(); p.setSector(s); p.setRowNumber(1); p.setPlotNumber(number); p.setPlotStatus(status); return plots.saveAndFlush(p);
    }
    private Request request(Customer c, Plot p, String cost, RequestStatus status) {
        var r = new Request(); r.setCustomer(c); r.setPlot(p); r.setTotalCost(new BigDecimal(cost)); r.setRequestStatus(status);
        r.setRequestDate(LocalDateTime.of(2026, 11, 14, 12, 0)); return requests.saveAndFlush(r);
    }
    private MockMultipartFile file(String text) { return new MockMultipartFile("file", "data.csv", "text/csv", text.getBytes(StandardCharsets.UTF_8)); }
    private String preview(String resource, String text, String mode) throws Exception {
        String response = mvc.perform(multipart("/api/" + resource + "/import/preview").file(file(text)).param("mode", mode))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.previewToken");
    }

    @Test
    void requestSearchCombinesFiltersSortsAndPaginatesInDatabase() throws Exception {
        var c = customer("Иван Петров"); var s = sector("Центральный"); var p = plot(s, 1, PlotStatus.FREE);
        request(c, p, "100", RequestStatus.NEW); var expensive = request(c, p, "300", RequestStatus.NEW);
        request(customer("Other"), p, "200", RequestStatus.APPROVED);
        mvc.perform(get("/api/requests/search").param("q", "пЕтРоВ").param("sectorId", s.getId().toString())
                        .param("requestStatus", "NEW").param("minCost", "50").param("maxCost", "400")
                        .param("fromDate", "2026-11-01T00:00:00").param("sort", "totalCost,desc", "requestDate,asc").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content[0].id").value(expensive.getId().toString()));
        mvc.perform(get("/api/requests/search").param("q", "петров").param("sort", "totalCost,desc").param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].totalCost").value(100));
        mvc.perform(get("/api/requests/search").param("page", "50"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void textSearchEscapesWildcardsAndSupportsArchiveAndAllResourceTypes() throws Exception {
        var c = customer("100% exact_name"); customer("Other name"); c.setStatus(Status.ARCHIVE); customers.saveAndFlush(c);
        var s = sector("Central"); plot(s, 1, PlotStatus.RESERVED);
        mvc.perform(get("/api/customers/search").param("q", "%").param("archiveStatus", "ARCHIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/customers/search").param("q", "' OR 1=1 --"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/sectors/search").param("q", "central")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/plots/search").param("sectorId", s.getId().toString()).param("plotStatus", "RESERVED").param("rowNumber", "1").param("minLengthCm", "150"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/employees/search").param("role", "WORKER").param("active", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsBadFiltersPaginationAndUnsafeSorting() throws Exception {
        for (String query : new String[]{"size=101", "page=-1", "requestStatus=INVALID", "minCost=10&maxCost=1", "sort=passwordHash", "sort=id,drop table", "sort=id,up"}) {
            mvc.perform(get("/api/requests/search?" + query)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/customers/search").param("minCost", "1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/employees/search").with(user("manager").roles("MANAGER"))).andExpect(status().isForbidden());
    }

    @Test
    void analyticsCalculatesMoneyOccupancyAndGroupsExcludingArchivedRecords() throws Exception {
        var c = customer("Customer"); var s = sector("Central"); var free = plot(s, 1, PlotStatus.FREE); var occupied = plot(s, 2, PlotStatus.OCCUPIED);
        var archivedPlot = plot(s, 3, PlotStatus.OCCUPIED); archivedPlot.setStatus(Status.ARCHIVE); plots.saveAndFlush(archivedPlot);
        request(c, free, "100", RequestStatus.NEW); request(c, occupied, "300", RequestStatus.COMPLETED);
        var archived = request(c, occupied, "1000", RequestStatus.REJECTED); archived.setStatus(Status.ARCHIVE); requests.saveAndFlush(archived);
        mvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requests.total").value(3))
                .andExpect(jsonPath("$.requests.active").value(2)).andExpect(jsonPath("$.requests.archived").value(1))
                .andExpect(jsonPath("$.totalRequestCost").value(400)).andExpect(jsonPath("$.averageRequestCost").value(200))
                .andExpect(jsonPath("$.occupiedPlotPercent").value(50)).andExpect(jsonPath("$.requestsBySector[0].count").value(2));
    }

    @Test
    void emptyAnalyticsReturnsZeroInsteadOfNullOrDivisionByZero() throws Exception {
        mvc.perform(get("/api/analytics/summary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRequestCost").value(0)).andExpect(jsonPath("$.occupiedPlotPercent").value(0))
                .andExpect(jsonPath("$.requests.total").value(0));
    }

    @Test
    void csvPreviewMakesNoChangesAndApplyImportsOnlyValidRows() throws Exception {
        String csv = "fullName,phone,email\r\n\"Иван, Петров\",+79990000000,ivan@example.com\r\n,123,invalid-email\r\n";
        String response = mvc.perform(multipart("/api/customers/import/preview").file(file(csv)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.validRows").value(1)).andExpect(jsonPath("$.invalidRows").value(1))
                .andExpect(jsonPath("$.errors[0].row").value(3)).andReturn().getResponse().getContentAsString();
        assertThat(customers.count()).isZero();
        String token = JsonPath.read(response, "$.previewToken");
        mvc.perform(multipart("/api/customers/import/apply").file(file(csv)).param("previewToken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.succeeded").value(1)).andExpect(jsonPath("$.failed").value(1));
        assertThat(customers.findAll().getFirst().getFullName()).isEqualTo("Иван, Петров");
        mvc.perform(multipart("/api/customers/import/apply").file(file(csv)).param("previewToken", token)).andExpect(status().isBadRequest());
        assertThat(customers.count()).isEqualTo(1);
    }

    @Test
    void csvApplyRequiresPreviewOfSameFileModeAndEmployee() throws Exception {
        String csv = "fullName,phone\r\nCustomer,123\r\n";
        String token = preview("customers", csv, "NEW_ONLY");
        mvc.perform(multipart("/api/customers/import/apply").file(file(csv + "Another,123\r\n")).param("previewToken", token)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/customers/import/apply").file(file(csv)).param("mode", "UPSERT").param("previewToken", token)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/customers/import/apply").file(file(csv)).with(user("other-admin").roles("ADMIN")).param("previewToken", token)).andExpect(status().isBadRequest());
        assertThat(customers.count()).isZero();
    }

    @Test
    void csvExportRespectsFiltersEscapesFormulasAndCanBeReimported() throws Exception {
        var c = customer("=2+2"); customer("Other");
        String exported = mvc.perform(get("/api/customers/export").param("q", "=2+2").param("size", "1"))
                .andExpect(status().isOk()).andExpect(header().string("Content-Disposition", "attachment; filename=\"customers.csv\""))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(exported).contains("'=2+2", "'+79990000000").doesNotContain("Other");
        String changed = exported.replace("'=2+2", "Updated");
        String token = preview("customers", changed, "UPSERT");
        mvc.perform(multipart("/api/customers/import/apply").file(file(changed)).param("mode", "UPSERT").param("previewToken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.succeeded").value(1));
        assertThat(customers.findById(c.getId()).orElseThrow().getFullName()).isEqualTo("Updated");
        assertThat(customers.findById(c.getId()).orElseThrow().getPhone()).isEqualTo("+79990000000");
    }

    @Test
    void csvValidatesReferencesMalformedFilesAndInvalidDates() throws Exception {
        mvc.perform(multipart("/api/customers/import/preview").file(file("fullName,fullName\nA,B\n"))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/customers/import/preview").file(file("passwordHash\nsecret\n"))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/plots/import/preview").file(file("sectorId,rowNumber,plotNumber,plotStatus,lengthCm,widthCm\n11111111-1111-4111-8111-111111111111,1,1,FREE,200,100\n")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.invalidRows").value(1));
        mvc.perform(multipart("/api/requests/import/preview").file(file("requestDate\ninvalid-date\n")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.invalidRows").value(1));
        mvc.perform(multipart("/api/customers/import/preview").file(file("fullName,phone\nOnlyName\n")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.invalidRows").value(1));
    }

    @Test
    void employeeCsvNeverReturnsPasswordsAndImportRequiresAdmin() throws Exception {
        String csv = "fullName,role,login,password,active\nEmployee,WORKER,worker,TemporaryPassword123!,true\n";
        String token = preview("employees", csv, "NEW_ONLY");
        mvc.perform(multipart("/api/employees/import/apply").file(file(csv)).param("previewToken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.succeeded").value(1));
        assertThat(passwords.matches("TemporaryPassword123!", employees.findByLogin("worker").orElseThrow().getPasswordHash())).isTrue();
        String exported = mvc.perform(get("/api/employees/export")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(exported).doesNotContain("password", "TemporaryPassword", "$2a$");
        mvc.perform(multipart("/api/employees/import/preview").file(file(csv)).with(user("manager").roles("MANAGER"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/customers/import/preview").file(file("fullName,phone\nA,123\n")).with(user("worker").roles("WORKER"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/sectors/import/preview").file(file("name\nA\n")).with(user("manager").roles("MANAGER"))).andExpect(status().isForbidden());
    }

    @Test
    void auditCapturesCrudWithCorrelationAndIsAdminOnly() throws Exception {
        String response = mvc.perform(post("/api/customers").header("X-Correlation-ID", "test-correlation")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Customer\",\"phone\":\"123\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("X-Correlation-ID", "test-correlation"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");
        mvc.perform(delete("/api/customers/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/audit").param("entityId", id).param("action", "CREATE").param("correlationId", "test-correlation"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actorLogin").value("admin"));
        mvc.perform(get("/api/admin/audit").with(user("manager").roles("MANAGER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/audit").param("sort", "passwordHash")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/audit").param("q", "Archive").param("entityId", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void csvImportsRelatedSectorsPlotsAndRequestsAndRoundTripsMultilineText() throws Exception {
        String sectorCsv = "name\nCentral\n";
        String sectorToken = preview("sectors", sectorCsv, "NEW_ONLY");
        mvc.perform(multipart("/api/sectors/import/apply").file(file(sectorCsv)).param("previewToken", sectorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.succeeded").value(1));
        var sector = sectors.findAll().getFirst();
        String plotCsv = "sectorId,rowNumber,plotNumber,plotStatus,lengthCm,widthCm\n" + sector.getId() + ",2,3,FREE,200,100\n";
        String plotToken = preview("plots", plotCsv, "NEW_ONLY");
        mvc.perform(multipart("/api/plots/import/apply").file(file(plotCsv)).param("previewToken", plotToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.succeeded").value(1));
        var customer = customer("Customer"); var plot = plots.findAll().getFirst();
        String csv = "customerId,plotId,requestStatus,requestDate,totalCost,note\n" + customer.getId() + "," + plot.getId()
                + ",NEW,2026-11-14T12:00:00,125.50,\"Первая строка\nВторая строка\"\n";
        String token = preview("requests", csv, "NEW_ONLY");
        mvc.perform(multipart("/api/requests/import/apply").file(file(csv)).param("previewToken", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.succeeded").value(1));
        assertThat(requests.findAll().getFirst().getNote()).isEqualTo("Первая строка\nВторая строка");
        String exported = mvc.perform(get("/api/requests/export")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String changed = exported.replace("125.50", "250.75");
        String update = preview("requests", changed, "UPSERT");
        mvc.perform(multipart("/api/requests/import/apply").file(file(changed)).param("mode", "UPSERT").param("previewToken", update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.succeeded").value(1));
        assertThat(requests.findAll().getFirst().getTotalCost()).isEqualByComparingTo("250.75");
    }

    @Test
    void rolledBackDeletionDoesNotLeaveSuccessfulAuditEvent() throws Exception {
        var s = sector("Protected sector"); plot(s, 1, PlotStatus.FREE);
        mvc.perform(delete("/api/sectors/" + s.getId())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/sectors/" + s.getId() + "/permanent")).andExpect(status().isConflict());
        assertThat(sectors.existsById(s.getId())).isTrue();
        assertThat(audit.findAll()).hasSize(1);
        assertThat(audit.findAll().getFirst().getAction()).isEqualTo("ARCHIVE");
    }

    @Test
    void auditPersistsSuccessfulAndFailedLoginsWithoutSecrets() throws Exception {
        var e = new Employee(); e.setFullName("Employee"); e.setLogin("audit-user"); e.setPasswordHash(passwords.encode("TemporaryPassword123!")); employees.saveAndFlush(e);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"audit-user\",\"password\":\"incorrect-password\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"audit-user\",\"password\":\"TemporaryPassword123!\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/audit").param("actorId", e.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("TemporaryPassword123!"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("incorrect-password"))));
    }
}
