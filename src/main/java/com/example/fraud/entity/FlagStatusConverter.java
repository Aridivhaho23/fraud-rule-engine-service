package com.example.fraud.entity;

import jakarta.persistence.Converter;

/** JPA converter that stores {@link FlagStatus} as text (see {@link EnumNameConverter}). */
@Converter
public class FlagStatusConverter extends EnumNameConverter<FlagStatus> {

    public FlagStatusConverter() {
        super(FlagStatus.class);
    }
}
