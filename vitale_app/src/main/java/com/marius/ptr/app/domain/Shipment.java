package com.marius.ptr.app.domain;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("shipment")
public record Shipment(

        @Id
        Long id,

        String trackingNumber,

        String invoiceNumber,

        @NotBlank(message = "The carrier must be defined")
        String carrier,

        ShipmentStatus status,

        String originWarehouse,

        @NotBlank(message = "The destination address must be defined")
        String destinationAddress,

        Instant shippedDate,

        Instant deliveredDate,

        @CreatedDate
        Instant createdDate,

        @LastModifiedDate
        Instant lastModifiedDate,

        @Version
        int version
) {
    public static Shipment create(String trackingNumber, String invoiceNumber, String carrier, String originWarehouse, String destinationAddress) {
        return new Shipment(null, trackingNumber, invoiceNumber, carrier, ShipmentStatus.PENDING,
                originWarehouse, destinationAddress, null, null, Instant.now(), Instant.now(), 0);
    }

    public Shipment packed() {
        return new Shipment(id, trackingNumber, invoiceNumber, carrier, ShipmentStatus.PACKED,
                originWarehouse, destinationAddress, shippedDate, deliveredDate, createdDate, lastModifiedDate, version);
    }

    public Shipment shipped() {
        return new Shipment(id, trackingNumber, invoiceNumber, carrier, ShipmentStatus.IN_TRANSIT,
                originWarehouse, destinationAddress, Instant.now(), deliveredDate, createdDate, lastModifiedDate, version);
    }

    public Shipment delivered() {
        return new Shipment(id, trackingNumber, invoiceNumber, carrier, ShipmentStatus.DELIVERED,
                originWarehouse, destinationAddress, shippedDate, Instant.now(), createdDate, lastModifiedDate, version);
    }
}
