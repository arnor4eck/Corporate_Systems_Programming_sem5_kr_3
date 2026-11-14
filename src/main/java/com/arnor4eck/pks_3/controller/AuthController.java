package com.arnor4eck.pks_3.controller;

import com.arnor4eck.pks_3.domain.Employee;
import com.arnor4eck.pks_3.dto.AuthDtos.*;
import com.arnor4eck.pks_3.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Авторизация", description = "Вход, refresh-сессии, текущий сотрудник и смена пароля")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;
    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Вход по логину и паролю", description = "Access JWT: 15 минут; refresh: 7 дней. После 5 неудач блокировка на 15 минут. Временный пароль требует смены.")
    public Tokens login(@Valid @RequestBody Login input) { return service.login(input); }
    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Обновить токены", description = "Refresh передаётся в JSON. Старый refresh и его access JWT отзываются. Повторное использование даёт 401.")
    public Tokens refresh(@Valid @RequestBody Refresh input) { return service.refresh(input); }
    @PostMapping("/logout")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Выйти из сессии", description = "Refresh передаётся в JSON. Отзывает сессию и access JWT. Повторный выход также возвращает 204.")
    public void logout(@Valid @RequestBody Refresh input) { service.logout(input); }
    @GetMapping("/me")
    @Operation(summary = "Текущий сотрудник", description = "UUID, логин, ФИО, роль и passwordChangeRequired. Пароли и хеши не возвращаются.")
    public User me(@Parameter(hidden = true) @AuthenticationPrincipal Employee employee) { return AuthService.user(employee); }
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Сменить пароль", description = "Нужен текущий пароль. Новый: от 12 символов до 72 байт UTF-8, отличается от старого. Отзывает все сессии; затем войдите повторно.")
    public void changePassword(@Parameter(hidden = true) @AuthenticationPrincipal Employee employee, @Valid @RequestBody ChangePassword input) {
        service.changePassword(employee.getId(), input);
    }
}
