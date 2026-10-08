package com.arnor4eck.pks_3.data;

import com.arnor4eck.pks_3.data.provider.ConcreteDataProvider;
import com.arnor4eck.pks_3.data.repository.EntityRepository;
import com.arnor4eck.pks_3.exception.entity.NotArchivedException;
import org.junit.jupiter.api.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.arnor4eck.pks_3.data.entity.Status.ACTIVE;
import static com.arnor4eck.pks_3.data.entity.Status.ARCHIVE;
import static org.junit.jupiter.api.Assertions.*;

public class ConcreteDataProviderTest {

    private static ConcreteDataProvider<TestEntity> provider;

    private static EntityRepository<TestEntity> repository;

    @BeforeAll
    public static void setUp() {
        repository = new EntityRepositoryStub();
        provider = new ConcreteDataProvider<>(repository);
    }

    @AfterEach
    public void tearDown() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("При сохранении сущности, она должна быть добавлена в базу данных")
    public void testSave() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity expected = new TestEntity(id, ACTIVE);

        // Act
        provider.save(expected);

        // Assert
        TestEntity actual = provider.get(id).get();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("При обновлении сущности, должна быть обновлена существующая сущность")
    public void testUpdate() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity expected = new TestEntity(id, ACTIVE);
        repository.save(expected);

        // Act
        TestEntity updated = new TestEntity(id, ARCHIVE);
        provider.update(updated);

        // Assert
        TestEntity actual = provider.get(id).get();
        assertEquals(1, provider.getAll().size());
        assertEquals(updated.getId(), actual.getId());
        assertEquals(updated.getStatus(), actual.getStatus());
    }

    @Test
    @DisplayName("При архивировании сущности, должен меняться ее статус, она не должна удаляться из базы")
    public void testArchive() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity expected = new TestEntity(id, ACTIVE);
        repository.save(expected);

        // Act
        provider.archive(id);

        // Assert
        TestEntity actual = provider.get(id).get();

        assertEquals(expected.getId(), actual.getId());
        assertEquals(ARCHIVE, actual.getStatus());
        assertEquals(1, provider.getAll().size());
    }

    @Test
    @DisplayName("При восстановлении сущности из архива, если она не архивирована, должно выброситься исключение")
    public void testRestoreNotArchived() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity expected = new TestEntity(id, ACTIVE);
        repository.save(expected);

        // Act + Assert
        Assertions.assertThrows(NotArchivedException.class, () -> provider.restore(id));
    }

    @Test
    @DisplayName("При восстановлении сущности из архива, должен меняться её статус")
    public void testRestoreArchived() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity expected = new TestEntity(id, ARCHIVE);
        repository.save(expected);

        // Act
        provider.restore(id);

        // Assert
        TestEntity actual = provider.get(id).get();

        assertEquals(expected.getId(), actual.getId());
        assertEquals(ACTIVE, actual.getStatus());
        assertEquals(1, provider.getAll().size());
    }

    @Test
    @DisplayName("При удалении сущности, которая не является архивированной, должно выбрасываться исключение")
    public void testDeleteNotArchived() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity expected = new TestEntity(id, ACTIVE);
        repository.save(expected);

        // Act + Assert
        assertThrows(NotArchivedException.class, () -> provider.delete(id));
    }

    @Test
    @DisplayName("При удалении сущности, она должна удалиться из репозитория")
    public void testDeleteArchived() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity expected = new TestEntity(id, ARCHIVE);
        repository.save(expected);

        // Act
        provider.delete(id);

        // Assert
        assertTrue(provider.getAll().isEmpty());
    }

    @Test
    @DisplayName("При запросе всех записей должны возвращаться все записи, хранящиеся в репозитории")
    public void testContainsAllEntities() {
        // Arrange
        List<TestEntity> expected = List.of(
                new TestEntity(ACTIVE),
                new TestEntity(ACTIVE),
                new TestEntity(ACTIVE)
        );
        repository.saveAll(expected);

        // Act
        Collection<TestEntity> actual = provider.getAll();

        // Assert
        assertIterableEquals(
            expected,
            actual
        );
    }

    @Test
    @DisplayName("При запросе сущности, существующей в базе данных, должна возвращаться эта сущность")
    public void testGetByIdExists() {
        // Arrange
        UUID id = UUID.randomUUID();
        TestEntity expected = new TestEntity(id, ACTIVE);
        repository.save(expected);

        // Act
        Optional<TestEntity> actual = provider.get(id);

        // Assert
        assertTrue(actual.isPresent());
    }
}
