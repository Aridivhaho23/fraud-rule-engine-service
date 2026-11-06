package com.example.fraud.entity;

import jakarta.persistence.Converter;

/** JPA converter that stores {@link MerchantCategory} as text (see {@link EnumNameConverter}). */
@Converter
public class MerchantCategoryConverter extends EnumNameConverter<MerchantCategory> {

    public MerchantCategoryConverter() {
        super(MerchantCategory.class);
    }
}
