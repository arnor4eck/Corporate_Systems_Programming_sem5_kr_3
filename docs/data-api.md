# Данные и API КР №3

Полная инструкция запуска и авторизации: [README](../README.md). Swagger описывает все действующие маршруты. Ниже перечислены параметры, форматы и особенности совместимости.

## Поиск, сортировка и страницы

`GET /api/{resource}?page=0&size=20` или `/api/{resource}/search`. Ресурсы: customers, employees, sectors, plots, requests. Без query-параметров у базового пути возвращается старый массив; при любом параметре — страница `{content,page,size,totalElements,totalPages}`. Поиск всегда выполняется в БД. page ≥ 0, size 1–100.

| Параметр | Назначение |
| --- | --- |
| q | Поиск до 200 символов без учёта регистра; `%` и `_` экранируются как обычные символы |
| archiveStatus | ACTIVE / ARCHIVE; без параметра — оба |
| fromDate / toDate | Включительный диапазон ISO LocalDateTime: requestDate у заявок, createdAt у клиентов/сотрудников/секторов |
| sort | Повторяемый `sort=totalCost,desc&sort=requestDate,asc`, максимум четыре поля; id всегда добавлен для стабильного порядка |
| code / urgent / priority / tagId | Внешний код, срочность, вычисляемый NORMAL/HIGH/URGENT, UUID метки — у заявок |
| requestStatus / customerId / employeeId / plotId | Фильтры заявок |
| sectorId / plotStatus | Фильтры мест и заявок |
| minCost / maxCost | Включительный диапазон стоимости заявок |
| rowNumber / plotNumber / minLengthCm / maxLengthCm | Фильтры мест |
| role / active | Фильтры сотрудников, только ADMIN |
| enabled | Включённые/отключённые сектора |

Фильтры объединяются через AND, q ищет через OR по текстовым полям. Неприменимые фильтры, неправильные UUID/enum/даты, обратные диапазоны и неподдерживаемая сортировка дают 400.

Сортировка:

- customers: id, status, fullName, phone, email, createdAt.
- employees: id, status, fullName, login, role, active, createdAt.
- sectors: id, status, name, createdAt.
- plots: id, status, sector.name, rowNumber, plotNumber, lengthCm, widthCm, plotStatus.
- requests: id, status, code, title, urgent, requestStatus, requestDate, processedDate, totalCost, createdAt, updatedAt, deceasedFullName, customer.fullName, plot.sector.name.

## Версии и предметные поля

PUT всех пяти сущностей требует текущую version из GET. Отсутствие — 400, устаревшая версия — 409. Нельзя просто подставлять 0 после первого изменения. Version есть также у комментариев; смена меток и bulk принимают версию заявки. Включение/отключение сектора принимает его версию.

Request.code — отдельный уникальный внешний идентификатор, 1–50 символов `[A-Za-z0-9._-]`; при отсутствии создаётся `REQ-<UUID>`. Title до 150 символов, непустой; при отсутствии нового названия используется серверное значение. urgent — логический признак срочности. priority рассчитывается по правилу из README, не принимается как редактируемое поле. createdAt/updatedAt/sectorId выводятся сервером.

## Массовые операции, метки и комментарии

POST `/api/requests/bulk`: operation, items (до 100 объектов id/version), опциональные requestStatus/tagId в зависимости от операции. Операции ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG. Ответ: succeeded, failed, results (id, success, status, error, record). Каждая запись — отдельная транзакция; дубли UUID внутри запроса возвращают ошибку.

GET/POST `/api/tags` — каталог с пагинацией/создание. POST `/api/requests/{id}/tags` принимает tagId/version. DELETE `/api/requests/{id}/tags/{tagId}?version=...` снимает метку.

GET/POST `/api/requests/{id}/notes` — история/создание служебного комментария. PUT/DELETE `/api/requests/{id}/notes/{noteId}` — изменение/удаление с version. Автор определяется по текущему сотруднику, нельзя передать чужой authorId. Сохраняются текст до 4000 символов, pinned, createdAt/updatedAt, version. Сначала выводятся закреплённые, затем новые. Изменять может автор или ADMIN; WORKER только читает.

## CSV

GET `/api/{resource}/export` применяет те же фильтры и сортировку, но выгружает все найденные строки, независимо от page/size, до 5000 записей. Формат UTF-8/RFC 4180: запятая, кавычки, многострочные ячейки; при импорте допускается BOM. Максимальный файл 2 MiB.

POST `/api/{resource}/import/preview`: multipart file и mode. Ответ содержит totalRows, validRows, invalidRows, errors (номер записи и ошибки), previewToken. Предметные таблицы не изменяются, событие проверки пишется в аудит.

POST `/api/{resource}/import/apply`: **тот же** file, mode, previewToken. Токен одноразовый, 15 минут, связан с хешем файла/режимом/сотрудником; после перезапуска backend нужна новая проверка. Корректные строки применяются независимо, ошибочные возвращаются отдельно. Между preview/apply связи, ограничения и версии перепроверяются.

Режимы:

- NEW_ONLY: новый UUID назначает сервер; id должен быть пустым. Внешний code заявки можно задать заранее, но он должен быть новым.
- UPSERT: для заявки сначала ищется внешний **code**; id при наличии должен обозначать ту же запись. Существующий code обновляется, новый создаёт заявку. Для остальных сущностей ключ — id. Обновление требует **version**. Неизвестный непустой id — ошибка строки.

Имена колонок берутся из экспорта. Метки заявки — колонка tags с UUID через `|` (до 50), пустая ячейка очищает метки, отсутствующая колонка сохраняет их. Каталог меток и связанные клиент/место/сотрудник создаются заранее. priority, sectorId, createdAt и updatedAt при импорте вычисляются/заполняются сервером.

Пример обновления по коду без внутреннего id:

```csv
code,version,customerId,plotId,requestStatus,requestDate,totalCost,title,urgent
REQ-DEMO-001,2,11111111-1111-4111-8111-111111111111,22222222-2222-4222-8222-222222222222,PROCESSING,2026-11-15T12:00:00,12500.50,Оформление захоронения,true
```

Замените UUID и version действительными. Экспорт сотрудников не содержит пароль/хеш. При создании сотрудника через CSV дополнительно передаётся колонка password с временным паролем; при обновлении её можно опустить. Пароль сохраняется BCrypt, требуется смена при входе, ошибки и аудит не содержат его значения.

Значения с начальным `=`, `+`, `-`, `@`, табуляцией или CR при экспорте экранируются апострофом для табличных редакторов; импорт снимает это экранирование. Начальный обычный апостроф сохраняется при круговом экспорте/импорте.

Экспорт — как чтение сущности. Импорт клиентов/мест/заявок — MANAGER/ADMIN; импорт секторов и любые действия с сотрудниками — ADMIN.

## Аналитика

GET `/api/analytics/summary`: total/active/archived по пяти сущностям; enabledEmployees/disabledEmployees; totalRequestCost/averageRequestCost; occupiedPlotPercent; requestsByStatus/plotsByStatus/requestsBySector; urgentRequests/ordinaryRequests/requestsByPriority.

Суммы, занятость и группировки не включают архивные записи. Пустая база даёт нули, enum-группы возвращаются и с нулевым количеством. Вычисление приоритета совпадает с DTO и поиском. Используются агрегаты в БД.

## Аудит и ошибки

GET `/api/admin/audit`, только ADMIN. Фильтры: q (логин/описание), action, entityType, entityId, actorId, correlationId, from/to (UTC Instant), page, size. Sort: occurredAt, actorLogin, action, entityType; direction asc/desc; по умолчанию время по убыванию. До 100 событий на странице.

Журнал хранит сотрудника, действие, тип/UUID записи, время UTC, Correlation ID, краткое описание. Входы/неудачные входы, сессии, пароли без значений, CRUD, архив, CSV, bulk, метки и комментарии фиксируются без секретов. Откатившиеся изменения не оставляют успешного события.

X-Correlation-ID возвращается во всех HTTP-ответах. Свой идентификатор может состоять из 1–64 латинских букв/цифр/символов `._-`, иначе генерируется UUID. Он же находится в аудите и JSON ошибок.

Ошибки MVC/JWT используют application/problem+json с type, title, status, detail, instance, correlationId. 400 — неверные данные, 401 — нет действительной авторизации, 403 — нет прав/нужно сменить пароль, 404 — нет записи, 409 — версия/связи/уникальность/состояние. Stack trace и внутренние исключения клиенту не возвращаются.
