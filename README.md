# КР №3 — система управления кладбищем

Spring Boot REST backend для клиентов, сотрудников, секторов, мест захоронения и заявок на захоронение/бронирование. Основная сущность — **заявка**. Метки и служебные комментарии относятся к заявкам; автором комментария является существующий сотрудник.

Java 21, Spring Boot 4.1.1, PostgreSQL 17, Spring Data JPA, Flyway, Spring Security/JWT, BCrypt, OpenAPI/Swagger, Apache Commons CSV, Gradle Wrapper. H2 используется для тестов и явного демонстрационного профиля `local`.

## Запуск через Docker

Нужен запущенный Docker Desktop. Для подготовки `.env` удобен Node.js 22+:

```powershell
node scripts/setup-env.cjs
docker compose up -d --build --wait --wait-timeout 180
docker compose ps
```

Скрипт создаёт `.env` из шаблона при необходимости, генерирует отсутствующие секреты и не выводит их. Уже существующие параметры базы сохраняются. Альтернатива — самостоятельно скопировать `.env.example` в `.env`, задать пароль БД, временный пароль администратора и `JWT_SECRET` (Base64 от минимум 32 случайных байт).

Compose собирает backend из исходников, запускает PostgreSQL отдельным сервисом, ждёт его healthcheck и проверяет health backend. Порт приложения публикуется только на `127.0.0.1`. Данные PostgreSQL находятся в named volume.

| Назначение | Адрес |
| --- | --- |
| Health | http://localhost:8080/actuator/health |
| Swagger | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON / YAML | `/v3/api-docs`, `/v3/api-docs.yaml` |
| REST API | `/api` |

```powershell
docker compose down
docker compose up -d --wait
```

Обычный `down` сохраняет базу. `down -v` удаляет volume с данными — для обычной остановки не используется.

## Первый вход и Swagger

Swagger и JSON/YAML OpenAPI открываются без логина и пароля. Для запросов API нужна авторизация через JWT.

При пустой таблице сотрудников создаётся ADMIN из `ADMIN_LOGIN`/`ADMIN_PASSWORD` в `.env`. Для прикладных запросов API временный пароль нужно сменить; это можно сделать прямо в Swagger: выполнить `/api/auth/login`, вставить `accessToken` в **Authorize** без слова `Bearer`, затем вызвать `/api/auth/change-password`. Альтернатива — PowerShell без вывода токенов/паролей:

```powershell
$settings = ConvertFrom-StringData (Get-Content .env -Raw)
$baseUrl = 'http://localhost:' + $settings.SERVER_PORT
$session = Invoke-RestMethod "$baseUrl/api/auth/login" -Method Post -ContentType 'application/json' -Body (@{login=$settings.ADMIN_LOGIN; password=$settings.ADMIN_PASSWORD} | ConvertTo-Json)
$newPassword = [System.Net.NetworkCredential]::new('', (Read-Host 'Новый пароль: минимум 12 символов' -AsSecureString)).Password
Invoke-RestMethod "$baseUrl/api/auth/change-password" -Method Post -ContentType 'application/json' -Headers @{Authorization="Bearer $($session.accessToken)"} -Body (@{currentPassword=$settings.ADMIN_PASSWORD; newPassword=$newPassword} | ConvertTo-Json)
```

После смены пароля выполните `/api/auth/login` с новым паролем. Получите новый `accessToken` и вставьте его в **Authorize**, без слова `Bearer`.

Смена пароля отзывает все сессии. При повторном запуске bootstrap не перезаписывает существующие учётные записи. Если в вашей старой базе были фиктивные passwordHash, действующий администратор должен назначить таким сотрудникам временные пароли.

## Роли и авторизация

| Роль | Доступ |
| --- | --- |
| WORKER | Просмотр предметных данных, экспорт, аналитика, метки и история комментариев |
| MANAGER | Дополнительно создание/изменение клиентов, мест, заявок; архив/восстановление; импорт; bulk; метки и свои комментарии |
| ADMIN | Всё, включая сотрудников, справочник секторов, аудит, окончательное удаление и изменение любых комментариев |

| Метод/путь | Тело |
| --- | --- |
| POST `/api/auth/login` | login, password |
| POST `/api/auth/refresh` | refreshToken |
| POST `/api/auth/logout` | refreshToken |
| GET `/api/auth/me` | Текущий сотрудник, роль, passwordChangeRequired |
| POST `/api/auth/change-password` | currentPassword, newPassword |
| POST `/api/employees/{id}/reset-password` | temporaryPassword; только ADMIN |

Access JWT: 15 минут по умолчанию. Refresh: 7 дней, хешируется в БД, ротируется при обновлении; повторное использование отозванного токена запрещено. После 5 неудачных входов учётная запись блокируется на 15 минут. Отключённые/архивные сотрудники не могут войти. Пароли сохраняются BCrypt (cost 12) и не возвращаются в DTO/экспорте/аудите.

## CRUD, версии, поиск

Ресурсы: `/api/customers`, `/api/employees` (также `/api/admin/users`), `/api/sectors`, `/api/plots`, `/api/requests`.

| Метод и суффикс | Действие |
| --- | --- |
| GET базового пути | Список; с параметрами поиска/страницы — объект страницы |
| GET `/{id}` | Карточка |
| POST базового пути | Создание, 201 |
| PUT `/{id}` | Замена предметных полей, **version обязателен** |
| DELETE `/{id}` | Архив, 204 |
| POST `/{id}/restore` | Восстановление |
| DELETE `/{id}/permanent` | Окончательное удаление только архивной записи, только ADMIN |

`version` берётся из последнего GET. Устаревшая версия даёт 409, отсутствие версии при PUT — 400. Используется JPA optimistic locking; `updatedAt` обновляется автоматически. `createdAt`, UUID и архив через PUT не меняются.

Новые клиенты используют, например, `/api/requests?page=0&size=20&sort=totalCost,desc`. Псевдонимы `/api/{resource}/search` также работают. Без query-параметров сохранён старый ответ-массив для совместимости с существующими клиентами.

У заявки есть внешний уникальный `code`, `title`, `urgent`, `priority`, sectorId, tags, version и время создания/изменения. Код можно задать самостоятельно; иначе сервер выдаёт `REQ-<UUID>`. Название при отсутствии задаётся сервером. Сектор выводится из выбранного места и хранится отдельной FK-связью. Место с существующими заявками нельзя перенести в другой сектор (409).

Приоритет вычисляется: COMPLETED/REJECTED → NORMAL; остальные срочные → URGENT; PROCESSING без срочности → HIGH; остальные → NORMAL. Этот же алгоритм используется в ответах, фильтрах и аналитике.

Пример обновления заявки:

```json
{
  "version": 0,
  "customerId": "11111111-1111-4111-8111-111111111111",
  "plotId": "22222222-2222-4222-8222-222222222222",
  "requestStatus": "NEW",
  "requestDate": "2026-11-15T12:00:00",
  "totalCost": 12500.50,
  "title": "Бронирование места",
  "urgent": true
}
```

UUID замените реальными. Для бронирования данные умершего и ответственный сотрудник могут отсутствовать. Размеры/номера места положительны, стоимость неотрицательная с точностью 2 знака.

## Сектора, метки, комментарии и bulk

- PATCH `/api/sectors/{id}/enabled`: `{ "enabled": false, "version": 0 }`, только ADMIN. Данные не удаляются. Отключённый/архивный сектор не принимает новые назначения заявок; существующие заявки доступны.
- GET/POST `/api/tags`: каталог тематических меток; POST принимает `{ "name": "Документы проверены" }`.
- POST `/api/requests/{id}/tags`: `{ "tagId": "<UUID>", "version": 0 }`.
- DELETE `/api/requests/{id}/tags/{tagId}?version=...`.
- GET/POST `/api/requests/{id}/notes`: история и создание служебного комментария `{ "content": "...", "pinned": true }`. Автор — текущий сотрудник.
- PUT `/api/requests/{id}/notes/{noteId}`: content, pinned, version. DELETE того же пути с `?version=...`. Автор или ADMIN; WORKER не изменяет комментарии.
- POST `/api/requests/bulk`: до 100 UUID с версиями; результаты отдельно по каждой записи, транзакции независимы.

```json
{
  "operation": "SET_STATUS",
  "requestStatus": "PROCESSING",
  "items": [{ "id": "11111111-1111-4111-8111-111111111111", "version": 0 }]
}
```

Операции: ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG. Для операций с метками нужен tagId. Повторный UUID в одном запросе возвращает ошибку этого элемента. Ошибка одной записи не отменяет остальные.

## CSV, аналитика и аудит

CSV: GET `/api/{resource}/export`, multipart POST `/api/{resource}/import/preview` и `/import/apply`. До 2 MiB/5000 строк, UTF-8/RFC 4180. Preview не меняет предметные данные, возвращает ошибки и одноразовый токен на 15 минут. Apply повторно проверяет данные, применяет корректные строки в отдельных транзакциях.

NEW_ONLY создаёт новые записи. UPSERT заявок ищет по **внешнему code** (или id), остальные сущности — по id. Обновление требует version. Метки в CSV — UUID через `|`. Поля priority/sectorId/updatedAt/createdAt вычисляются или заполняются сервером. Токен проверки связан с файлом, режимом и сотрудником; после перезапуска нужна новая проверка.

GET `/api/analytics/summary`: количества всех/неархивных/архивных сущностей, сумма/средняя стоимость, занятость мест, срочные/обычные заявки, группировки по секторам, статусам и вычисляемому приоритету.

GET `/api/admin/audit`: входы/неудачные входы, сессии, пароли без их значений, CRUD/архив, CSV, метки, комментарии и bulk. Есть поиск/фильтры/сортировка/пагинация, только ADMIN. В каждом ответе — X-Correlation-ID; журнал сохраняет тот же идентификатор. Ошибки API/безопасности имеют единый ProblemDetail с type, title, status, detail, instance и correlationId; внутренние исключения/stack trace не возвращаются.

Полный список фильтров и форматы: [docs/data-api.md](docs/data-api.md). Сверка с КР №3: [docs/kr3-compliance.md](docs/kr3-compliance.md).

## Тесты и проверка на PostgreSQL

```powershell
# JDK 21, локальные тесты на разрешённой заданием H2
.\gradlew.bat test

# Изолированная проверка настоящих backend/PostgreSQL в Docker
powershell -ExecutionPolicy Bypass -File scripts/verify-compose.ps1
```

Проверочный Compose использует отдельные проект `kr3-verify`, базу/volume и порт 18081; основная база не затрагивается. Скрипт проверяет API и обычный down/up с сохранением данных. Тестовые секреты остаются в игнорируемой `.verification/`, не попадают в образ/Git и не печатаются. Тестовый стек остаётся запущенным для просмотра; остановить: `docker compose --env-file .verification/compose.env -p kr3-verify -f compose.verify.yaml down`.

Локальный запуск с PostgreSQL: загрузите переменные из `.env` в конфигурацию IDE/окружение и выполните `gradlew.bat bootRun`. Необязательный профиль H2: `gradlew.bat bootRun --args=--spring.profiles.active=local`.

Пакеты: controller, service, repository, domain, dto, security, filter, error, config. Flyway V1–V4 содержит предметные таблицы, роли, refresh-сессии, аудит, метки, связи и служебные комментарии. Hibernate проверяет схему (`validate`).
