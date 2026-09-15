package com.marius.ptr.app.exceptions;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(String invoiceNumber) {
        super("No invoice exists with number " + invoiceNumber);
    }
}
