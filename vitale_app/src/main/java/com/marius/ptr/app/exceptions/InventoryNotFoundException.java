package com.marius.ptr.app.exceptions;

public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(String isbn) {
        super("No inventory record exists for book with isbn " + isbn);
    }
}
