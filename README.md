# pks_3

Модели из [Corporate_Systems_Programming_sem5](https://github.com/arnor4eck/Corporate_Systems_Programming_sem5/tree/2579648c01bba4138047cc9a39f34fec1e6f0984/src/main/java/com/arnor4eck/model)
перенесены в `com.arnor4eck.pks_3.data.entity` как JPA-сущности Hibernate.
Для Customer, Employee, Sector, Plot и Request созданы Spring Data JPA репозитории.

* Идентификаторы — UUID согласно существующему интерфейсу Entity этого проекта.
* Plot.sector, Request.customer, Request.plot и Request.employee — связи ManyToOne
  с ленивой загрузкой. Сотрудник в заявке необязателен; каскадного удаления связанных сущностей нет.
* Status в каждой сущности отвечает за архив (колонка archive_status).
  Plot.plotStatus и Request.requestStatus хранят предметные статусы в колонке status.
* Перечисления хранятся строками, стоимость — BigDecimal / DECIMAL(10,2).
  Размеры места — целые сантиметры, как в исходной SQL-схеме.
* update меняет предметные поля, сохраняя UUID, дату создания и статус архива.
  Архивирование и восстановление выполняет ConcreteDataProvider.
* Flyway создаёт таблицы, внешние ключи, индексы и ограничения уникальности.
  Hibernate проверяет соответствие схемы через ddl-auto=validate.

По умолчанию используется H2 в памяти. Для PostgreSQL задайте переменные окружения
DB_URL (например, jdbc:postgresql://localhost:5432/pks_3), DB_USERNAME и DB_PASSWORD.
Миграция V1 рассчитана на новую пустую базу. Перенос существующих записей из старой
базы с числовыми ID и PostgreSQL ENUM не выполняется.

Запуск: `./gradlew bootRun` (Windows: `./gradlew.bat bootRun`), требуется JDK 21.
Проверка: `./gradlew test`. Интеграционные тесты проверяют сохранение и повторную
загрузку связей, необязательные поля, уникальность места и архивирование.
