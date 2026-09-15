package com.marius.ptr.app.web;

import com.marius.ptr.app.domain.Shipment;
import com.marius.ptr.app.domain.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public List<Shipment> getByInvoiceNumber(@RequestParam String invoiceNumber) {
        return shipmentService.findByInvoiceNumber(invoiceNumber);
    }

    @GetMapping("{trackingNumber}")
    public Shipment getByTrackingNumber(@PathVariable String trackingNumber) {
        return shipmentService.findByTrackingNumber(trackingNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Shipment create(@Valid @RequestBody CreateShipmentRequest request) {
        return shipmentService.createShipment(
                request.invoiceNumber(), request.carrier(), request.originWarehouse(), request.destinationAddress());
    }

    @PostMapping("{trackingNumber}/pack")
    public Shipment pack(@PathVariable String trackingNumber) {
        return shipmentService.markPacked(trackingNumber);
    }

    @PostMapping("{trackingNumber}/ship")
    public Shipment ship(@PathVariable String trackingNumber) {
        return shipmentService.markShipped(trackingNumber);
    }

    @PostMapping("{trackingNumber}/deliver")
    public Shipment deliver(@PathVariable String trackingNumber) {
        return shipmentService.markDelivered(trackingNumber);
    }
}
