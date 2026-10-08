package com.arnor4eck.pks_3.data;

import com.arnor4eck.pks_3.data.entity.Entity;
import com.arnor4eck.pks_3.data.entity.Status;

import java.util.UUID;

public class TestEntity implements Entity<TestEntity> {

    private UUID id;
    private Status status;

    public TestEntity(
        UUID id,
        Status status
    ) {
        this.id = id;
        this.status = status;
    }

    public TestEntity(
        Status status
    ) {
        this(UUID.randomUUID(), status);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void update(TestEntity other) {
        this.status = other.status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }
}
