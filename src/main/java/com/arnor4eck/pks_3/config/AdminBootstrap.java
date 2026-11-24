package com.arnor4eck.pks_3.config;

import com.arnor4eck.pks_3.domain.*;
import com.arnor4eck.pks_3.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final EmployeeRepository employees;
    private final PasswordEncoder passwords;
    private final String login;
    private final String password;
    public AdminBootstrap(EmployeeRepository employees, PasswordEncoder passwords,
                          @Value("${app.auth.admin-login:admin}") String login,
                          @Value("${app.auth.admin-password:}") String password) {
        this.employees = employees;
        this.passwords = passwords;
        this.login = login;
        this.password = password;
    }
    @Override
    @Transactional
    public void run(String... args) {
        if (password.isBlank() || employees.count() != 0) return;
        if (password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("ADMIN_PASSWORD must contain at least 12 characters and at most 72 UTF-8 bytes");
        var employee = new Employee();
        employee.setLogin(login);
        employee.setFullName("Administrator");
        employee.setRole(Role.ADMIN);
        employee.setPasswordHash(passwords.encode(password));
        employees.save(employee);
    }
}
