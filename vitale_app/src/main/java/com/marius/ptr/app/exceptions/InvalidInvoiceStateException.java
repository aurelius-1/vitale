package com.marius.ptr.app.exceptions;

import com.marius.ptr.app.domain.InvoiceStatus;

public class InvalidInvoiceStateException extends RuntimeException {

    public InvalidInvoiceStateException(String invoiceNumber, InvoiceStatus currentStatus, String attemptedAction) {
        super("Cannot %s invoice %s because it is %s".formatted(attemptedAction, invoiceNumber, currentStatus));
    }
}
