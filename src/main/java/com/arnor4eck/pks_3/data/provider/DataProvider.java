package com.arnor4eck.pks_3.data.provider;

import com.arnor4eck.pks_3.data.entity.Entity;

import java.util.Collection;
import java.util.UUID;

/**
 * 3. CRUD и архив
 * */
public interface DataProvider<T extends Entity> {
    // Получение списка
    Collection<T> getAll();

    // Получение одной записи по UUID.
    T get(UUID id);

    // Создание
    void save(T val);

    // Изменение.
    void update(T val);

    // Архивирование вместо обычного удаления
    void archive(UUID id);

    // Восстановление из архива
    void restore(UUID id);

    // Окончательное удаление только администратором
    void delete(UUID id);

    // Проверка version при изменении записи - ???
}
