package com.arnor4eck.pks_3.repository;

import com.arnor4eck.pks_3.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, EntityRepository<Employee> {
    java.util.Optional<Employee> findByLogin(String login);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Employee e where e.login = :login")
    java.util.Optional<Employee> lockByLogin(@org.springframework.data.repository.query.Param("login") String login);
}

