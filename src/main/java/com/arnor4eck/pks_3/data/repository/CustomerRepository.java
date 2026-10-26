package com.arnor4eck.pks_3.data.repository;

import com.arnor4eck.pks_3.data.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, EntityRepository<Customer> {
}

