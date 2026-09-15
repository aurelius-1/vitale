package com.marius.ptr.app.domain;

import org.springframework.data.annotation.Id;

public record InvoiceLine(

        @Id
        Long id,

        String isbn,

        String title,

        int quantity,

        Double unitPrice
) {
    public double lineTotal() {
        return unitPrice * quantity;
    }
}
