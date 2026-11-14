package com.arnor4eck.pks_3.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Objects;

public final class Versions {
    private Versions() {}
    public static void require(Long actual, Long expected) {
        if (expected == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "version is required for an update");
        if (!Objects.equals(actual, expected)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Record has changed; reload it and retry");
    }
}
