package com.arnor4eck.pks_3.service;

import com.arnor4eck.pks_3.domain.*;
import com.arnor4eck.pks_3.repository.*;
import com.arnor4eck.pks_3.dto.AuthDtos.*;
import com.arnor4eck.pks_3.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final EmployeeRepository employees;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final AuditService audit;
    // A valid BCrypt hash ensures unknown logins also perform a password check.
    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Tokens login(Login input) {
        return issue(verifyCredentials(input));
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Employee verifyCredentials(Login input) {
        checkBytes(input.password());
        Employee employee = employees.lockByLogin(input.login()).orElse(null);
        boolean matches = passwords.matches(input.password(), employee == null ? DUMMY_HASH : employee.getPasswordHash());
        if (employee == null || !available(employee)) {
            audit.authentication(employee, input.login(), false);
            throw unauthorized();
        }
        if (employee.getLockedUntil() != null && employee.getLockedUntil().isAfter(Instant.now())) {
            audit.authentication(employee, input.login(), false);
            throw unauthorized();
        }
        if (!matches) {
            int failures = employee.getLockedUntil() != null ? 1 : employee.getFailedLogins() + 1;
            employee.setFailedLogins(failures);
            employee.setLockedUntil(failures >= 5 ? Instant.now().plusSeconds(900) : null);
            audit.authentication(employee, input.login(), false);
            throw unauthorized();
        }
        employee.setFailedLogins(0);
        employee.setLockedUntil(null);
        audit.authentication(employee, input.login(), true);
        return employee;
    }

    public Tokens refresh(Refresh input) {
        var session = sessions.lockByTokenHash(TokenService.hash(input.refreshToken())).orElseThrow(AuthService::unauthorized);
        if (!valid(session)) throw unauthorized();
        session.setRevoked(true);
        audit.session(session.getEmployee(), "REFRESH", "Session rotated");
        return issue(session.getEmployee());
    }

    public void logout(Refresh input) {
        sessions.lockByTokenHash(TokenService.hash(input.refreshToken())).ifPresent(s -> {
            s.setRevoked(true);
            audit.session(s.getEmployee(), "LOGOUT", "Session revoked");
        });
    }

    public void changePassword(UUID id, ChangePassword input) {
        checkBytes(input.currentPassword());
        checkBytes(input.newPassword());
        var employee = employees.findById(id).orElseThrow(AuthService::unauthorized);
        if (!passwords.matches(input.currentPassword(), employee.getPasswordHash())) throw unauthorized();
        if (passwords.matches(input.newPassword(), employee.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must differ from the current password");
        employee.setPasswordHash(passwords.encode(input.newPassword()));
        employee.setPasswordChangeRequired(false);
        sessions.revokeAll(id);
        audit.record("CHANGE_PASSWORD", "employees", id, "Password changed; sessions revoked");
    }

    private Tokens issue(Employee employee) {
        String refresh = TokenService.randomToken();
        var session = new RefreshSession();
        session.setEmployee(employee);
        session.setTokenHash(TokenService.hash(refresh));
        session.setExpiresAt(Instant.now().plusSeconds(tokens.refreshSeconds));
        sessions.save(session);
        return new Tokens(tokens.access(session), refresh, "Bearer", tokens.accessSeconds, user(employee));
    }

    public static boolean available(Employee e) { return e.isActive() && e.getStatus() == Status.ACTIVE; }
    public static boolean valid(RefreshSession s) {
        return !s.isRevoked() && s.getExpiresAt().isAfter(Instant.now()) && available(s.getEmployee());
    }
    public static User user(Employee e) { return new User(e.getId(), e.getLogin(), e.getFullName(), e.getRole(), e.isPasswordChangeRequired()); }
    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials or session");
    }
    private static void checkBytes(String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password exceeds 72 UTF-8 bytes");
    }
}
