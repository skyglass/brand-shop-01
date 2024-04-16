package com.cakefactory.catalog.persistence;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "catalog")
class ItemEntity {

    @Id
    String sku;

    @NotBlank
    String title;

    @NotNull
    BigDecimal price;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemEntity) {
            ItemEntity other = (ItemEntity) obj;
            return Objects.equals(this.sku, other.sku);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.sku);
    }
}