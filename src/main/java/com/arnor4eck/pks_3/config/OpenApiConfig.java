package com.arnor4eck.pks_3.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;
import java.util.Map;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Система управления кладбищем — REST API", version = "1.0",
        description = """
                Backend контрольной работы №3. Авторизация, клиенты, сектора, места захоронения и заявки.

                1. Выполните POST /api/auth/login с логином и паролем.
                2. Скопируйте accessToken, нажмите Authorize и вставьте только токен (без префикса Bearer).
                3. Если passwordChangeRequired=true, сначала смените пароль через /api/auth/change-password,
                   затем войдите повторно. Смена пароля отзывает все сессии.
                4. Refresh-токен передаётся только в JSON тела /refresh или /logout.

                WORKER: просмотр. MANAGER: изменение клиентов, мест и заявок, архив и восстановление.
                ADMIN: все операции, управление сотрудниками и секторами, окончательное удаление.
                DELETE /{id} архивирует запись. /permanent удаляет только архивную запись.
                UUID связанных записей берите из ответов соответствующих API.

                Основная сущность темы — заявка на захоронение или бронирование. code — внешний ключ импорта.
                При PUT передавайте текущий version; устаревшая версия даёт 409.
                Приоритет: завершённые/отклонённые — NORMAL; срочные — URGENT; PROCESSING — HIGH; остальные NORMAL.
                GET списка с параметрами возвращает страницу; без параметров сохранён совместимый массив.
                Массовые операции /api/requests/bulk работают с UUID и version. Служебные комментарии
                /notes используют существующего сотрудника как автора; тематические метки находятся в /api/tags.
                """), security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
        description = "Access JWT из /api/auth/login или /api/auth/refresh. Вставьте только значение accessToken.")
public class OpenApiConfig {
    @Bean
    OpenApiCustomizer apiResponses() {
        return api -> {
            api.getComponents().addSchemas("ApiProblem", new ObjectSchema()
                    .description("Ошибка application/problem+json; стек и внутренние исключения не возвращаются")
                    .addProperty("type", new StringSchema().example("about:blank"))
                    .addProperty("title", new StringSchema().example("Bad Request"))
                    .addProperty("status", new IntegerSchema().example(400))
                    .addProperty("detail", new StringSchema().example("Invalid input"))
                    .addProperty("instance", new StringSchema().example("/api/requests")));
            api.getComponents().getSchemas().get("ApiProblem").addProperty("correlationId", new StringSchema().example("request-123"));
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                for (var error : Map.of("400", "Неверный JSON, UUID или параметры; ошибка валидации",
                        "401", "Нет авторизации, неверные учётные данные или истёкшая/отозванная сессия",
                        "403", "Недостаточно прав или требуется смена временного пароля",
                        "500", "Внутренняя ошибка сервера").entrySet()) {
                    operation.getResponses().addApiResponse(error.getKey(), problem(error.getValue()));
                }
                if (path.contains("{id}")) operation.getResponses().addApiResponse("404", problem("Запись с указанным UUID не найдена"));
                if (!path.startsWith("/api/auth/") && method != io.swagger.v3.oas.models.PathItem.HttpMethod.GET)
                    operation.getResponses().addApiResponse("409", problem("Конфликт уникальности, связи или состояния архива"));
                if (operation.getParameters() != null) operation.getParameters().stream()
                        .filter(p -> "id".equals(p.getName())).forEach(p -> p.description("UUID записи из ответа создания или списка")
                                .example("11111111-1111-4111-8111-111111111111"));
            }));
            var employees = api.getPaths().get("/api/employees").getGet();
            employees.getParameters().stream().filter(p -> "page".equals(p.getName()))
                    .forEach(p -> p.required(false).description("Номер страницы от 0. Без параметра возвращается массив всех сотрудников."));
            employees.getResponses().get("200").description("Страница сотрудников либо массив без параметра page")
                    .content(new Content().addMediaType("application/json", new MediaType().schema(new ComposedSchema()
                            .addOneOfItem(new Schema<>().$ref("#/components/schemas/EmployeePage"))
                            .addOneOfItem(new ArraySchema().items(new Schema<>().$ref("#/components/schemas/EmployeeResponse"))))));
        };
    }

    private static ApiResponse problem(String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType("application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiProblem"))));
    }
}
