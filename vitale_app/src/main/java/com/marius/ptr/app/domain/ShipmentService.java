package com.marius.ptr.app.domain;

import com.marius.ptr.app.exceptions.InvalidInvoiceStateException;
import com.marius.ptr.app.exceptions.InvalidShipmentStateException;
import com.marius.ptr.app.exceptions.ShipmentNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.function.UnaryOperator;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final InvoiceService invoiceService;

    public ShipmentService(ShipmentRepository shipmentRepository, InvoiceService invoiceService) {
        this.shipmentRepository = shipmentRepository;
        this.invoiceService = invoiceService;
    }

    public Shipment createShipment(String invoiceNumber, String carrier, String originWarehouse, String destinationAddress) {
        var invoice = invoiceService.findByInvoiceNumber(invoiceNumber);
        if (invoice.status() != InvoiceStatus.PAID) {
            throw new InvalidInvoiceStateException(invoiceNumber, invoice.status(), "ship (the invoice must be paid first)");
        }

        var trackingNumber = "TRK-" + Instant.now().toEpochMilli();
        return shipmentRepository.save(Shipment.create(trackingNumber, invoiceNumber, carrier, originWarehouse, destinationAddress));
    }

    public Shipment findByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException(trackingNumber));
    }

    public List<Shipment> findByInvoiceNumber(String invoiceNumber) {
        return shipmentRepository.findByInvoiceNumber(invoiceNumber);
    }

    public Shipment markPacked(String trackingNumber) {
        return transition(trackingNumber, ShipmentStatus.PENDING, ShipmentStatus.PACKED, Shipment::packed);
    }

    public Shipment markShipped(String trackingNumber) {
        return transition(trackingNumber, ShipmentStatus.PACKED, ShipmentStatus.IN_TRANSIT, Shipment::shipped);
    }

    public Shipment markDelivered(String trackingNumber) {
        return transition(trackingNumber, ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED, Shipment::delivered);
    }

    private Shipment transition(String trackingNumber, ShipmentStatus expected, ShipmentStatus next, UnaryOperator<Shipment> advance) {
        var shipment = findByTrackingNumber(trackingNumber);
        if (shipment.status() != expected) {
            throw new InvalidShipmentStateException(trackingNumber, shipment.status(), next);
        }
        return shipmentRepository.save(advance.apply(shipment));
    }
}
