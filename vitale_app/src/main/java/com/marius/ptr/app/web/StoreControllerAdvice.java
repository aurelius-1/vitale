package com.marius.ptr.app.web;

import com.marius.ptr.app.exceptions.InsufficientStockException;
import com.marius.ptr.app.exceptions.InvalidInvoiceStateException;
import com.marius.ptr.app.exceptions.InvalidShipmentStateException;
import com.marius.ptr.app.exceptions.InventoryNotFoundException;
import com.marius.ptr.app.exceptions.InvoiceNotFoundException;
import com.marius.ptr.app.exceptions.ShipmentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class StoreControllerAdvice {

    @ExceptionHandler(InventoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String inventoryNotFoundHandler(InventoryNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String insufficientStockHandler(InsufficientStockException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String invoiceNotFoundHandler(InvoiceNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(InvalidInvoiceStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String invalidInvoiceStateHandler(InvalidInvoiceStateException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String shipmentNotFoundHandler(ShipmentNotFoundException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(InvalidShipmentStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String invalidShipmentStateHandler(InvalidShipmentStateException ex) {
        return ex.getMessage();
    }
}
