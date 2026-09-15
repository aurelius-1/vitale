package com.marius.ptr.app.exceptions;

public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException(String trackingNumber) {
        super("No shipment exists with tracking number " + trackingNumber);
    }
}
