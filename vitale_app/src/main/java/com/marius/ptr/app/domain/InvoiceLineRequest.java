package com.marius.ptr.app.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record InvoiceLineRequest(

        @NotBlank(message = "The ISBN must be defined")
        String isbn,

        @Min(value = 1, message = "The quantity must be at least 1")
        int quantity
) {}
