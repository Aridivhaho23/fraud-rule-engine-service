package com.example.fraud.entity;

import jakarta.persistence.Converter;

/** JPA converter that stores {@link Severity} as text (see {@link EnumNameConverter}). */
@Converter
public class SeverityConverter extends EnumNameConverter<Severity> {

    public SeverityConverter() {
        super(Severity.class);
    }
}
