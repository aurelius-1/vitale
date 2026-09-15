package com.marius.ptr.app.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("inventory")
public record Inventory(

        @Id
        String isbn,

        @Min(value = 0, message = "The quantity on hand cannot be negative")
        int quantityOnHand,

        @NotBlank(message = "The warehouse location must be defined")
        String warehouseLocation,

        @Min(value = 0, message = "The reorder threshold cannot be negative")
        int reorderThreshold,

        @CreatedDate
        Instant createdDate,

        @LastModifiedDate
        Instant lastModifiedDate,

        @Version
        int version
) {
    public static Inventory of(String isbn, int quantityOnHand, String warehouseLocation, int reorderThreshold) {
        return new Inventory(isbn, quantityOnHand, warehouseLocation, reorderThreshold, Instant.now(), Instant.now(), 0);
    }

    public boolean isLowStock() {
        return quantityOnHand <= reorderThreshold;
    }

    public Inventory withQuantityOnHand(int newQuantity) {
        return new Inventory(isbn, newQuantity, warehouseLocation, reorderThreshold, createdDate, lastModifiedDate, version);
    }
}
