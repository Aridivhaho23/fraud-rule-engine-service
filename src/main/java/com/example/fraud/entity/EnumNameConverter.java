package com.example.fraud.entity;

import jakarta.persistence.AttributeConverter;

/**
 * Base class that stores any enum as its NAME in a VARCHAR column.
 *
 * <p>Why not {@code @Enumerated}? Newer Hibernate versions may choose a database-specific column
 * type for enums (e.g. a native ENUM on MySQL), which would differ between H2, PostgreSQL and MySQL.
 * An explicit converter keeps the mapping identical everywhere - important because this application
 * supports several databases. (Never store the ordinal number: re-ordering the enum would corrupt data.)
 *
 * @param <E> the enum type
 */
public abstract class EnumNameConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> enumType;

    protected EnumNameConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Enum.valueOf(enumType, dbData);
    }
}
