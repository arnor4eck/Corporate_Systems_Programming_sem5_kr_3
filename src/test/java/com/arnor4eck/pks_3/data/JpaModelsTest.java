package com.arnor4eck.pks_3.data;

import com.arnor4eck.pks_3.data.entity.*;
import com.arnor4eck.pks_3.data.provider.ConcreteDataProvider;
import com.arnor4eck.pks_3.data.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class JpaModelsTest {
    @Autowired CustomerRepository customers;
    @Autowired SectorRepository sectors;
    @Autowired PlotRepository plots;
    @Autowired EmployeeRepository employees;
    @Autowired RequestRepository requests;
    @Autowired EntityManager entityManager;

    private Plot createPlot() {
        Sector sector = new Sector();
        sector.setName("Central");
        sectors.save(sector);
        Plot plot = new Plot();
        plot.setSector(sector);
        plot.setRowNumber(1);
        plot.setPlotNumber(2);
        return plots.saveAndFlush(plot);
    }

    @Test
    void persistsGraphAndKeepsArchiveStatusSeparate() {
        Customer customer = new Customer();
        customer.setFullName("Customer");
        customer.setPhone("+71111111111");
        customers.save(customer);
        Employee employee = new Employee();
        employee.setFullName("Employee");
        employee.setLogin("manager");
        employee.setPasswordHash("test-hash");
        employees.save(employee);
        Plot plot = createPlot();
        plot.setPlotStatus(PlotStatus.RESERVED);
        Request request = new Request();
        request.setCustomer(customer);
        request.setPlot(plot);
        request.setEmployee(employee);
        request.setDeceasedBirthday(LocalDate.of(1950, 1, 2));
        request.setTotalCost(new BigDecimal("1250.45"));
        requests.saveAndFlush(request);
        var provider = new ConcreteDataProvider<>(requests);
        provider.archive(request.getId());
        entityManager.flush();
        entityManager.clear();

        Request loaded = requests.findById(request.getId()).orElseThrow();
        assertEquals(Status.ARCHIVE, loaded.getStatus());
        assertEquals(RequestStatus.NEW, loaded.getRequestStatus());
        assertEquals(PlotStatus.RESERVED, loaded.getPlot().getPlotStatus());
        assertEquals("Central", loaded.getPlot().getSector().getName());
        assertEquals(customer.getId(), loaded.getCustomer().getId());
        assertEquals(Role.MANAGER, loaded.getEmployee().getRole());
        assertEquals(new BigDecimal("1250.45"), loaded.getTotalCost());
        assertEquals(LocalDate.of(1950, 1, 2), loaded.getDeceasedBirthday());
        assertNotNull(loaded.getCreatedAt());
        provider.restore(loaded.getId());
        entityManager.flush();
        entityManager.clear();
        assertEquals(Status.ACTIVE, requests.findById(request.getId()).orElseThrow().getStatus());
    }

    @Test
    void allowsReservationWithoutEmployeeOrDeceased() {
        Customer customer = new Customer();
        customer.setFullName("Customer");
        customer.setPhone("123");
        customers.save(customer);
        Request request = new Request();
        request.setCustomer(customer);
        request.setPlot(createPlot());
        requests.saveAndFlush(request);
        entityManager.clear();
        Request loaded = requests.findById(request.getId()).orElseThrow();
        assertNull(loaded.getEmployee());
        assertNull(loaded.getDeceasedFullName());
    }

    @Test
    void rejectsDuplicatePlotLocation() {
        Plot existing = createPlot();
        Plot duplicate = new Plot();
        duplicate.setSector(existing.getSector());
        duplicate.setRowNumber(existing.getRowNumber());
        duplicate.setPlotNumber(existing.getPlotNumber());
        assertThrows(DataIntegrityViolationException.class, () -> plots.saveAndFlush(duplicate));
    }
}
