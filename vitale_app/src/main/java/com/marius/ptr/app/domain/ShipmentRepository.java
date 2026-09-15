package com.marius.ptr.app.domain;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends CrudRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    List<Shipment> findByInvoiceNumber(String invoiceNumber);
}
