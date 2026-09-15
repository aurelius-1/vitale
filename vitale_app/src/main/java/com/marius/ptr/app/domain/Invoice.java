package com.marius.ptr.app.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;

@Table("invoice")
public record Invoice(

        @Id
        Long id,

        String invoiceNumber,

        InvoiceStatus status,

        @MappedCollection(idColumn = "invoice_id")
        List<InvoiceLine> lines,

        Double totalAmount,

        Instant issuedDate,

        Instant paidDate,

        @CreatedDate
        Instant createdDate,

        @LastModifiedDate
        Instant lastModifiedDate,

        @Version
        int version
) {
    public static Invoice issue(String invoiceNumber, List<InvoiceLine> lines) {
        double total = lines.stream().mapToDouble(InvoiceLine::lineTotal).sum();
        return new Invoice(null, invoiceNumber, InvoiceStatus.ISSUED, lines, total, Instant.now(), null, Instant.now(), Instant.now(), 0);
    }

    public Invoice paid() {
        return new Invoice(id, invoiceNumber, InvoiceStatus.PAID, lines, totalAmount, issuedDate, Instant.now(), createdDate, lastModifiedDate, version);
    }

    public Invoice cancelled() {
        return new Invoice(id, invoiceNumber, InvoiceStatus.CANCELLED, lines, totalAmount, issuedDate, paidDate, createdDate, lastModifiedDate, version);
    }
}
