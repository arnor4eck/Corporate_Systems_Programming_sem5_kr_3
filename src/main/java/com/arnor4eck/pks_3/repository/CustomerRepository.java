package com.arnor4eck.pks_3.repository;

import com.arnor4eck.pks_3.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, EntityRepository<Customer> {
}

