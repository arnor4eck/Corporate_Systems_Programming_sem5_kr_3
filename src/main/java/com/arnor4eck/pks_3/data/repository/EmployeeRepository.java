package com.arnor4eck.pks_3.data.repository;

import com.arnor4eck.pks_3.data.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, EntityRepository<Employee> {
}

