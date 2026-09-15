package com.marius.ptr.app.exceptions;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String isbn, int requested, int available) {
        super("Cannot reserve %d unit(s) of book %s: only %d available".formatted(requested, isbn, available));
    }
}
