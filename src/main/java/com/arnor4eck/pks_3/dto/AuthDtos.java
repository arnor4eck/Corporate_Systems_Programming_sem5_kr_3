package com.arnor4eck.pks_3.dto;

import com.arnor4eck.pks_3.domain.Role;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}
    @Schema(name = "LoginRequest", description = "Учётные данные активного сотрудника")
    public record Login(
            @Schema(example = "admin") @NotBlank @Size(max = 50) String login,
            @Schema(description = "Не более 72 байт UTF-8", example = "YourPassword123!", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank @Size(max = 72) String password) {}
    @Schema(name = "RefreshRequest", description = "Refresh-токен из предыдущего ответа входа или обновления")
    public record Refresh(@Schema(example = "paste-refresh-token-from-login-response", accessMode = Schema.AccessMode.WRITE_ONLY)
                          @NotBlank @Size(max = 128) String refreshToken) {}
    @Schema(name = "ChangePasswordRequest")
    public record ChangePassword(
            @Schema(example = "YourPassword123!", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank @Size(max = 72) String currentPassword,
            @Schema(description = "От 12 символов до 72 байт UTF-8; должен отличаться от текущего", example = "NewUniquePassword123!", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank @Size(min = 12, max = 72) String newPassword) {}
    @Schema(name = "CurrentEmployee", description = "Безопасные данные текущего сотрудника")
    public record User(UUID id, String login, String fullName, Role role,
                       @Schema(description = "true: разрешены только me, change-password и logout", example = "false")
                       boolean passwordChangeRequired) {}
    @Schema(name = "ResetPasswordRequest")
    public record ResetPassword(
            @Schema(description = "Временный пароль: от 12 символов до 72 байт UTF-8", example = "NewTemporaryPassword123!", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank @Size(min = 12, max = 72) String temporaryPassword) {}
    @Schema(name = "AuthTokens", description = "Выданные токены. Не сохраняйте их в общедоступных логах")
    public record Tokens(
            @Schema(description = "Короткоживущий JWT для заголовка Authorization: Bearer <accessToken>") String accessToken,
            @Schema(description = "Одноразовый при обновлении opaque-токен для JSON-тела refresh/logout") String refreshToken,
            @Schema(example = "Bearer") String tokenType,
            @Schema(description = "Срок действия access JWT в секундах", example = "900") long expiresIn, User user) {}
}
