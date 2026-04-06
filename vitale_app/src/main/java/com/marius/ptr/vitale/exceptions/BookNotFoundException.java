package com.marius.ptr.vitale.exceptions;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(String isbn) {
        super("The book with isbn " + isbn + " was not found");
    }
}
