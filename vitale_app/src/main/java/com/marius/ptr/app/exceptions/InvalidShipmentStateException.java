package com.marius.ptr.app.exceptions;

import com.marius.ptr.app.domain.ShipmentStatus;

public class InvalidShipmentStateException extends RuntimeException {

    public InvalidShipmentStateException(String trackingNumber, ShipmentStatus currentStatus, ShipmentStatus attemptedStatus) {
        super("Cannot move shipment %s to %s because it is currently %s".formatted(trackingNumber, attemptedStatus, currentStatus));
    }
}
