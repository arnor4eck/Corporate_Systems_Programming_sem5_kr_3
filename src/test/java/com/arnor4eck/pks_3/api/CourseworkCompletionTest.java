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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @WithMockUser(username="manager", roles="MANAGER")
@Sql(statements={"DELETE FROM request_comments","DELETE FROM request_tags","DELETE FROM tags","DELETE FROM audit_events","DELETE FROM refresh_sessions","DELETE FROM requests","DELETE FROM plots","DELETE FROM employees","DELETE FROM customers","DELETE FROM sectors"}, executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements={"DELETE FROM request_comments","DELETE FROM request_tags","DELETE FROM tags","DELETE FROM audit_events","DELETE FROM refresh_sessions","DELETE FROM requests","DELETE FROM plots","DELETE FROM employees","DELETE FROM customers","DELETE FROM sectors"}, executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CourseworkCompletionTest {
    @Autowired MockMvc mvc;
    @Autowired RequestRepository requests;
    @Autowired CustomerRepository customers;
    @Autowired PlotRepository plots;
    @Autowired SectorRepository sectors;
    @Autowired EmployeeRepository employees;
    private Request request;
    private Employee manager;
    @BeforeEach void seed() {
        manager=new Employee();manager.setLogin("manager");manager.setFullName("Manager");manager.setPasswordHash("unused-test-hash");manager=employees.saveAndFlush(manager);
        var customer=new Customer();customer.setFullName("Customer");customer.setPhone("123");customer=customers.saveAndFlush(customer);
        var sector=new Sector();sector.setName("Central");sector=sectors.saveAndFlush(sector);
        var plot=new Plot();plot.setSector(sector);plot.setRowNumber(1);plot.setPlotNumber(1);plot=plots.saveAndFlush(plot);
        request=new Request();request.setCustomer(customer);request.setPlot(plot);request.setRequestDate(LocalDateTime.of(2026,11,14,12,0));request.setTotalCost(new BigDecimal("100"));request=requests.saveAndFlush(request);
    }
    private String payload(long version, boolean urgent, String status) {
        return "{\"customerId\":\""+request.getCustomer().getId()+"\",\"plotId\":\""+request.getPlot().getId()+"\",\"requestStatus\":\""+status+"\",\"requestDate\":\"2026-11-14T12:00:00\",\"totalCost\":100,\"version\":"+version+",\"urgent\":"+urgent+",\"title\":\"Updated request\"}";
    }
    @Test void staleVersionReturns409AndProtectsStoredChanges() throws Exception {
        mvc.perform(put("/api/requests/"+request.getId()).contentType(MediaType.APPLICATION_JSON).content(payload(request.getVersion(),true,"PROCESSING")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.priority").value("URGENT")).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.updatedAt").isNotEmpty());
        mvc.perform(put("/api/requests/"+request.getId()).contentType(MediaType.APPLICATION_JSON).content(payload(request.getVersion(),false,"NEW")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.type").value("about:blank")).andExpect(jsonPath("$.correlationId").isNotEmpty());
        assertThat(requests.findById(request.getId()).orElseThrow().isUrgent()).isTrue();
    }
    @Test void calculatedPriorityMatchesSearchAndAnalytics() throws Exception {
        request.setUrgent(true);request=requests.saveAndFlush(request);
        mvc.perform(get("/api/requests").param("urgent","true").param("priority","URGENT").param("page","0").param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].code").value(request.getCode()));
        mvc.perform(get("/api/analytics/summary")).andExpect(status().isOk()).andExpect(jsonPath("$.urgentRequests").value(1))
                .andExpect(jsonPath("$.requestsByPriority[?(@.key=='URGENT')].count").value(org.hamcrest.Matchers.contains(1)));
        request.setRequestStatus(RequestStatus.COMPLETED);request=requests.saveAndFlush(request);
        mvc.perform(get("/api/requests/search").param("priority","NORMAL")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        assertThat(request.getPriority()).isEqualTo(Priority.NORMAL);
    }
    @Test void bulkReturnsPartialSuccessAndChecksVersionAndPermissions() throws Exception {
        String body="{\"operation\":\"ARCHIVE\",\"items\":[{\"id\":\""+request.getId()+"\",\"version\":0},{\"id\":\"11111111-1111-4111-8111-111111111111\",\"version\":0}]}";
        mvc.perform(post("/api/requests/bulk").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded").value(1)).andExpect(jsonPath("$.failed").value(1)).andExpect(jsonPath("$.results[1].status").value(404));
        mvc.perform(post("/api/requests/bulk").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.results[0].status").value(409));
        mvc.perform(post("/api/requests/bulk").with(user("worker").roles("WORKER")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/requests/bulk").contentType(MediaType.APPLICATION_JSON).content("{\"operation\":\"SET_STATUS\",\"items\":[{\"id\":\""+request.getId()+"\",\"version\":1}]}"))
                .andExpect(status().isBadRequest());
    }
    @Test void requestLabelsCanBeAddedFilteredAndRemoved() throws Exception {
        String created=mvc.perform(post("/api/tags").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Срочное оформление\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String tag=JsonPath.read(created,"$.id");
        String changed=mvc.perform(post("/api/requests/"+request.getId()+"/tags").contentType(MediaType.APPLICATION_JSON).content("{\"tagId\":\""+tag+"\",\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tags[0].name").value("Срочное оформление")).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/requests/search").param("tagId",tag)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        Number version=JsonPath.read(changed,"$.version");
        mvc.perform(delete("/api/requests/"+request.getId()+"/tags/"+tag).param("version",version.toString())).andExpect(status().isOk()).andExpect(jsonPath("$.tags").isEmpty());
        mvc.perform(get("/api/requests/search").param("tagId",tag)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void commentsKeepExistingEmployeeAuthorAndPinAndProtectEdits() throws Exception {
        String path="/api/requests/"+request.getId()+"/notes";
        String created=mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Связаться с клиентом\",\"pinned\":true}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.authorId").value(manager.getId().toString())).andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String id=JsonPath.read(created,"$.id");
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].pinned").value(true));
        mvc.perform(put(path+"/"+id).with(user("other").roles("MANAGER")).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Changed\",\"version\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(path+"/"+id).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Клиент уведомлён\",\"pinned\":false,\"version\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(delete(path+"/"+id).param("version","0")).andExpect(status().isConflict());
        mvc.perform(delete(path+"/"+id).param("version","1")).andExpect(status().isNoContent());
    }
    @Test void csvUpsertFindsExistingRequestByExternalCode() throws Exception {
        String csv="code,version,customerId,plotId,requestStatus,requestDate,totalCost,urgent\n"+request.getCode()+",0,"+request.getCustomer().getId()+","+request.getPlot().getId()+",NEW,2026-11-14T12:00:00,999,true\n";
        var file=new MockMultipartFile("file","requests.csv","text/csv",csv.getBytes(StandardCharsets.UTF_8));
        String result=mvc.perform(multipart("/api/requests/import/preview").file(file).param("mode","UPSERT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.invalidRows").value(0)).andReturn().getResponse().getContentAsString();
        String preview=JsonPath.read(result,"$.previewToken");
        mvc.perform(multipart("/api/requests/import/apply").file(file).param("mode","UPSERT").param("previewToken",preview))
                .andExpect(status().isOk()).andExpect(jsonPath("$.succeeded").value(1)).andExpect(jsonPath("$.results[0].id").value(request.getId().toString()));
        assertThat(requests.count()).isEqualTo(1);assertThat(requests.findById(request.getId()).orElseThrow().getTotalCost()).isEqualByComparingTo("999");
    }
    @Test void disabledSectorPreventsNewRequestsAndUsersAliasIsAdminOnly() throws Exception {
        var sector=sectors.findAll().getFirst();
        mvc.perform(patch("/api/sectors/"+sector.getId()+"/enabled").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false,\"version\":"+sector.getVersion()+"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(payload(0,false,"NEW"))).andExpect(status().isConflict());
        mvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").with(user("admin").roles("ADMIN")).param("page","0")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test void csvRoundTripRetainsLabelsAndDetectsStalePreviewVersion() throws Exception {
        String tagJson=mvc.perform(post("/api/tags").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"CSV label\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String tag=JsonPath.read(tagJson,"$.id");
        mvc.perform(post("/api/requests/"+request.getId()+"/tags").contentType(MediaType.APPLICATION_JSON).content("{\"tagId\":\""+tag+"\",\"version\":0}"))
                .andExpect(status().isOk());
        String csv=mvc.perform(get("/api/requests/export").param("code",request.getCode())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains(tag);
        var file=new MockMultipartFile("file","requests.csv","text/csv",csv.getBytes(StandardCharsets.UTF_8));
        String previewJson=mvc.perform(multipart("/api/requests/import/preview").file(file).param("mode","UPSERT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.invalidRows").value(0)).andReturn().getResponse().getContentAsString();
        String token=JsonPath.read(previewJson,"$.previewToken");
        mvc.perform(multipart("/api/requests/import/apply").file(file).param("mode","UPSERT").param("previewToken",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.succeeded").value(1));
        mvc.perform(get("/api/requests/"+request.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.tags[0].id").value(tag));
        mvc.perform(multipart("/api/requests/import/preview").file(file).param("mode","UPSERT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.invalidRows").value(1));
    }
    @Test void allBulkActionsRestoreChangeStatusAndModifyTags() throws Exception {
        String tagJson=mvc.perform(post("/api/tags").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bulk label\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String tag=JsonPath.read(tagJson,"$.id");
        for(String operation:List.of("ADD_TAG","REMOVE_TAG","SET_STATUS","ARCHIVE","RESTORE")) {
            long version=requests.findById(request.getId()).orElseThrow().getVersion();
            String json="{\"operation\":\""+operation+"\",\"tagId\":\""+tag+"\",\"requestStatus\":\"PROCESSING\",\"items\":[{\"id\":\""+request.getId()+"\",\"version\":"+version+"}]}";
            mvc.perform(post("/api/requests/bulk").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.succeeded").value(1)).andExpect(jsonPath("$.failed").value(0));
        }
        assertThat(requests.findById(request.getId()).orElseThrow().getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(requests.findById(request.getId()).orElseThrow().getRequestStatus()).isEqualTo(RequestStatus.PROCESSING);
    }
    @Test void allErrorSourcesExposeSameProblemFields() throws Exception {
        mvc.perform(get("/api/admin/audit")).andExpect(status().isForbidden()).andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").isNotEmpty()).andExpect(jsonPath("$.instance").value("/api/admin/audit")).andExpect(jsonPath("$.correlationId").isNotEmpty());
        mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").isNotEmpty()).andExpect(jsonPath("$.instance").value("/api/requests")).andExpect(jsonPath("$.correlationId").isNotEmpty());
    }
    @Test void referencedPlotCannotMoveAndBreakRequestSectorRelationship() throws Exception {
        var sector=new Sector();sector.setName("Other sector");sector=sectors.saveAndFlush(sector);
        String body="{\"sectorId\":\""+sector.getId()+"\",\"rowNumber\":1,\"plotNumber\":1,\"plotStatus\":\"FREE\",\"lengthCm\":200,\"widthCm\":100,\"version\":0}";
        mvc.perform(put("/api/plots/"+request.getPlot().getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}
