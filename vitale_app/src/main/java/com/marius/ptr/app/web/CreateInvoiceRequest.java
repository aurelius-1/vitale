package com.marius.ptr.app.web;

import com.marius.ptr.app.domain.InvoiceLineRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateInvoiceRequest(

        @NotEmpty(message = "An invoice must contain at least one line item")
        @Valid
        List<InvoiceLineRequest> lines
) {}
