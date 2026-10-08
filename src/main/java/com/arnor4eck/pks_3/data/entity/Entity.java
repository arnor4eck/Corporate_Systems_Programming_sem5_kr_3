package com.arnor4eck.pks_3.data.entity;

import java.util.UUID;

public interface Entity<T extends Entity<T>> {
    UUID getId();
    Status getStatus();
    void setStatus(Status status);
    void update(T other);
}
