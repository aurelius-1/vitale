package com.marius.ptr.app.web;

import jakarta.validation.constraints.NotBlank;

public record CreateShipmentRequest(

        @NotBlank(message = "The invoice number must be defined")
        String invoiceNumber,

        @NotBlank(message = "The carrier must be defined")
        String carrier,

        @NotBlank(message = "The origin warehouse must be defined")
        String originWarehouse,

        @NotBlank(message = "The destination address must be defined")
        String destinationAddress
) {}
